import React, {useState, useRef, useEffect, useImperativeHandle, useCallback} from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeSanitize from 'rehype-sanitize'
import './Chat.css'
import type {ChatMessage, ChatResponse} from "../../api/aimo-client/AimoClientModel";
import {Message} from "./ChatModels";
import Loader from "../loader/Loader"
import TipsAndUpdatesOutlined from '@mui/icons-material/TipsAndUpdatesOutlined';
import ConstructionOutlined from '@mui/icons-material/ConstructionOutlined';
import {Button} from "@mui/material";
import {useTheme} from "@mui/material/styles";
import {UnsetCountCaller} from "../../utils/UnsetCountCaller";

/**
 * Stable row identity. The API uses the same messageId for USER and ASSISTANT within one request
 * (both 1, both 2, …), so the key must include role/type as well as requestId.
 */
function chatRowKey(res: ChatResponse, msg: ChatMessage): string {
    if (res.responseId != null) {
        return `${res.responseId}:${msg.messageId}:${msg.type}`
    }
    return `L:${msg.messageId}`
}

function typeSortOrder(type: ChatMessage['type']): number {
    switch (type) {
        case 'USER':
            return 0
        case 'SYSTEM':
            return 1
        case 'ASSISTANT':
            return 2
        case 'TOOL':
            return 3
        default:
            return 9
    }
}

function sortKeyChatOrder(m: Message): [number, number, number] {
    let t = 0
    if (m.response.createdAt != null) {
        const p = m.response.createdAt instanceof Date 
            ? m.response.createdAt.getTime() 
            : Date.parse(m.response.createdAt as any)
        if (!Number.isNaN(p)) {
            t = p
        }
    }
    return [t, m.message.messageId, typeSortOrder(m.message.type)]
}

/**
 * Props for the Chat component.
 *
 * @property onSend Optional callback that is invoked when the *user* explicitly
 * submits a message via the UI (click Send / Enter).
 *
 * @property initialResponses Optional pre-seeded responses to populate the chat.
 * These are added into the internal Map on mount (keeps insertion order).
 */
interface ChatProps {
    onSend?: (msg: ChatMessage) => void
    initialResponses?: ChatResponse[]
}

/**
 * Imperative handle exposed by the `Chat` component via `forwardRef`.
 *
 * Purpose:
 * - Describes the methods a parent can call on the Chat component instance.
 * - Intended for programmatic control from a parent (e.g. adding a response,
 *   appending streaming text to an existing message).
 *
 * Notes for callers (behavioral contract):
 * - `addResponse` is used to insert an entire ChatResponse object into the chat.
 *   It extracts all messages from the response and displays them.
 * - `appendMessage` is used to append additional text to an already-existing
 *   message identified by response context (streaming/partial-update use case).
 *   Callers must ensure the target message exists first.
 */
export type ChatHandle = {
    busy: () => (() => void)
    disableInput: () => (() => void)

    /**
     * Append text to an existing message within a response.
     *
     * What to use it for:
     * - Streaming responses that arrive in chunks: create a response (via `addResponse`)
     *   then repeatedly call `appendMessage` to append each chunk to that message.
     *
     * Caller expectations:
     * - `response` is the ChatResponse containing the message.
     * - `incoming.messageId` identifies which message within that response.
     * - `incoming.content` and `incoming.thinking` contain the pieces to append.
     */
    appendMessage: (response: ChatResponse, incoming: ChatMessage) => void

    /**
     * Insert a new ChatResponse into the chat.
     *
     * What to use it for:
     * - Programmatically adding a complete response with all its messages.
     * - Each message in the response will be displayed in the chat UI.
     *
     * Caller expectations:
     * - Provide a complete `ChatResponse` object with all its messages.
     */
    addResponse: (response: ChatResponse) => void

    setResponses: (responses: ChatResponse[]) => void
}

/**
 * Chat component
 *
 * Purpose:
 * - Renders a chat UI and provides a programmatic API to the parent via a forwarded ref.
 *
 * Props summary (passed via the function parameters below):
 * - `onSend?: (msg: ChatMessage) => void` — optional callback invoked when the *user* submits a message from the UI.
 * - `initialResponses?: ChatResponse[]` — optional array of responses used to seed the chat on mount.
 *
 * Ref/API:
 * - The component forwards a ref whose type is `ChatHandle`. The parent can call methods
 *   on that handle (for example `addResponse` to insert a response or `appendMessage`
 *   to stream/append text to an existing message).
 */
const Chat = React.forwardRef<ChatHandle, ChatProps>(function Chat({onSend, initialResponses = []}: ChatProps, ref) {

    /** Mirrors `messages` state so imperative add/append can run back-to-back before React re-renders (streaming). */
    const messagesRef = useRef<Map<string, Message>>(new Map())

    /** Messages displayed in the chat */
    const [messages, setMessages] = useState<Map<string, Message>>(() => {
        const m = new Map<string, Message>()
        for (const response of initialResponses) {
            for (const msg of response.messages) {
                m.set(chatRowKey(response, msg), {
                    response: response,
                    message: msg,
                    expandThinking: false,
                    expandTool: false,
                })
            }
        }
        messagesRef.current = m
        return m
    })

    /** Current input value */
    const [input, setInput] = useState('')
    const [inputEnabled, setInputEnabled] = useState(true);
    const [busy, setBusy] = useState(false);

    /** Ref to the messages container for scrolling */
    const containerRef = useRef<HTMLDivElement | null>(null)

    // flag to indicate that the next messages update should trigger a smooth scroll
    const shouldScrollRef = useRef(false)
    // last scrollTop to detect direction (user scrolled up/down)
    const lastScrollTopRef = useRef<number>(0)
    const BOTTOM_THRESHOLD = 40 // px from bottom considered "at bottom"

    /**
     * Scroll-to-bottom effect for the messages container.
     *
     * Purpose:
     * - When the component's `messages` state is updated and the `shouldScrollRef`
     *   flag is set, this effect scrolls the messages container to the bottom.
     */
    useEffect(() => {
        if (!shouldScrollRef.current) return

        const el = containerRef.current
        if (!el) {
            shouldScrollRef.current = false
            return
        }

        // run after paint to allow smooth scrolling without a jump
        // run after the next paint and ensure layout is stable by nesting rAFs
        const raf = window.requestAnimationFrame.bind(window)
        raf(() => {
            raf(() => {
                el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
            })
        })
    }, [messages, busy])


    // Listen for scroll direction. If user scrolls up -> disable auto-scroll.
    // If user scrolls (down) and reaches the bottom -> re-enable auto-scroll.
    useEffect(() => {
        const el = containerRef.current
        if (!el) return

        // initialize lastScrollTop
        lastScrollTopRef.current = el.scrollTop

        const onScroll = () => {
            const scrollTop = el.scrollTop
            const isScrollingUp = scrollTop < lastScrollTopRef.current
            const distanceFromBottom = el.scrollHeight - (scrollTop + el.clientHeight)
            const atBottom = distanceFromBottom <= BOTTOM_THRESHOLD

            if (atBottom) {
                // user scrolled to bottom: re-enable auto-scroll
                shouldScrollRef.current = true
            } else if (isScrollingUp) {
                // user scrolled up: stop auto-scrolling
                shouldScrollRef.current = false
            }

            lastScrollTopRef.current = scrollTop
        }

        el.addEventListener('scroll', onScroll, {passive: true})
        return () => el.removeEventListener('scroll', onScroll)
    }, [])


    /**
     * Insert an entire ChatResponse into the chat state.
     *
     * Purpose / When to use:
     * - Use this method to programmatically add a complete response to the chat UI.
     * - Each message within the response will be displayed.
     *
     * Parameter:
     * @param response - A ChatResponse object containing all messages to display.
     *
     * Caller expectations:
     * - Provide a complete `ChatResponse` object with all its messages populated.
     *
     * Return:
     * - This callback does not return a value; it enqueues the response into the chat state.
     */
    const addResponse = useCallback((response: ChatResponse) => {
        // request a smooth scroll for this update only
        shouldScrollRef.current = true
        const next = new Map(messagesRef.current)
        for (const msg of response.messages) {
            next.set(chatRowKey(response, msg), {
                response: response,
                message: msg,
                expandThinking: false,
                expandTool: false,
            })
        }
        messagesRef.current = next
        setMessages(next)
    }, [])

    /**
     * Create a user message and add it as a single-message response.
     *
     * Purpose:
     * - Used when the user submits a message from the UI.
     */
    const addMessage = useCallback((msg: ChatMessage) => {
        // Create a minimal response wrapper for the message
        const response: ChatResponse = {
            chatId: '',
            responseId: `local-${Date.now()}`,
            messages: [msg],
            createdAt: new Date(),
        }
        addResponse(response)
    }, [addResponse])

    /**
     * Append additional text to an existing message within a response.
     *
     * Purpose:
     * - Used for streaming or partial updates where new content arrives in chunks
     *   and should be appended to an already-rendered message.
     *
     * Parameters:
     * @param response - The ChatResponse containing the message to append to.
     * @param incoming - A ChatMessage object containing the `messageId` of the
     *                   target message and the `content`/`thinking` to append.
     *
     * Caller expectations / contract:
     * - `response` and `messageId` must refer to an existing message previously inserted
     *   via `addResponse`. The caller is responsible for creating that initial message before appending.
     * - The `content` and `thinking` provided will be appended to the existing message's text.
     * - This operation intentionally does NOT trigger an automatic scroll.
     *
     * Behavior notes:
     * - The implementation merges text by concatenation.
     * - If no message exists with the provided id, the current implementation
     *   treats it as a new message.
     */
    const appendMessage = useCallback((response: ChatResponse, incoming: ChatMessage) => {
        const next = new Map(messagesRef.current)
        const key = chatRowKey(response, incoming)
        const existing = next.get(key)

        if (existing) {
            const appendedThinking = incoming.thinking ? `${existing.message.thinking ?? ''}${incoming.thinking}` : existing.message.thinking
            const appendedContent = incoming.content ? `${existing.message.content ?? ''}${incoming.content}` : existing.message.content
            next.set(key, {
                response: existing.response,
                message: {
                    ...existing.message,
                    content: appendedContent,
                    thinking: appendedThinking,
                    toolName: incoming.toolName ?? existing.message.toolName,
                },
                expandThinking: existing.expandThinking,
                expandTool: existing.expandTool,
            })
        } else {
            // No row yet; add it with the provided response
            next.set(key, {
                response: response,
                message: {...incoming},
                expandThinking: false,
                expandTool: false,
            })
        }
        messagesRef.current = next
        setMessages(next)
    }, [])

    const _setResponses = useCallback((responses: ChatResponse[]) => {
        shouldScrollRef.current = true
        const m = new Map<string, Message>()
        for (const response of responses) {
            for (const msg of response.messages) {
                m.set(chatRowKey(response, msg), {
                    response: response,
                    message: msg,
                    expandThinking: false,
                    expandTool: false,
                })
            }
        }
        messagesRef.current = m
        setMessages(m)
    }, [])


    const inputEnabledCounter = useRef( new UnsetCountCaller(() => {
        setInputEnabled(false)
    }, () => {
        setInputEnabled(true)
    }));

    const busyCounter = useRef( new UnsetCountCaller(() => {
        setBusy(true)
    }, () => {
        setBusy(false)
    }));

    /**
     * Expose the component's imperative API to a parent via `ref`.
     *
     * Exposed methods (ChatHandle):
     * - addResponse: insert a new response and all its messages into the chat.
     *   Usage: parentRef.current?.addResponse({ responseId, messages, ... })
     *
     * - appendMessage: append/update an existing message within a response.
     *   Usage: parentRef.current?.appendMessage({ messageId, requestId, content, ... })
     *
     * Contract & notes for callers:
     * - Call `addResponse` to add a complete response with all its messages.
     * - Call `appendMessage` to append partial/streamed text to an already-created message.
     * - The parent can call these through a ref typed as `ChatHandle`.
     * - The dependency array ensures the handle is updated if the internal callbacks change.
     */
    useImperativeHandle(ref, () => ({
        appendMessage: appendMessage,
        addResponse: addResponse,
        setResponses: _setResponses,
        disableInput: () => inputEnabledCounter.current.doSet(),
        busy: () => busyCounter.current.doSet(),
    }), [appendMessage, addResponse, _setResponses, inputEnabledCounter, busyCounter])

    function handleSubmit(e?: React.FormEvent) {
        if (!inputEnabled) return
        if (e) e.preventDefault()
        // construct a Message and pass it to addMessage
        const toAdd: ChatMessage = {
            messageId: Date.now(),
            type: 'USER',
            content: input,
        }
        addMessage(toAdd)
        setInput('')
        if (toAdd && onSend) onSend(toAdd)
    }

    function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault()
            if (!inputEnabled) return
            handleSubmit()
        }
    }

    const messageList = Array.from(messages.values()).sort((a, b) => {
        const [ta, ia, ua] = sortKeyChatOrder(a)
        const [tb, ib, ub] = sortKeyChatOrder(b)
        if (ta !== tb) return ta - tb
        if (ia !== ib) return ia - ib
        return ua - ub
    })

    const theme = useTheme()
    return (
        <div className="chat">

            {messages.size <= 0 ? (
                <div className="chat__list_container" ref={containerRef} style={ { marginTop: theme.mixins.toolbar.minHeight} }>
                    Splash Screen
                </div>
            ) : (
                <div className="chat__list_container" ref={containerRef} style={ { marginTop: theme.mixins.toolbar.minHeight} }>
                    <div className="chat__list">
                        {messageList.map((m, index) => {
                            const msg = m.message
                            const isUser = msg.type === 'USER'
                            const isTool = msg.type === 'TOOL'
                            const rowClass =
                                isUser ? 'user' : isTool ? 'tool' : 'assistant'

                            const rowKey = chatRowKey(m.response, msg)

                            // Show timestamp only on the last message of each response group
                            const isLastInGroup =
                                index === messageList.length - 1 ||
                                messageList[index + 1].response.responseId !== m.response.responseId

                            const toggleThinking = () => {
                                const next = new Map(messagesRef.current)
                                const existing = next.get(rowKey)
                                if (existing) {
                                    next.set(rowKey, {
                                        ...existing,
                                        expandThinking: !existing.expandThinking,
                                    })
                                    messagesRef.current = next
                                    setMessages(next)
                                }
                            }

                            const toggleTool = () => {
                                const next = new Map(messagesRef.current)
                                const existing = next.get(rowKey)
                                if (existing) {
                                    next.set(rowKey, {
                                        ...existing,
                                        expandTool: !existing.expandTool,
                                    })
                                    messagesRef.current = next
                                    setMessages(next)
                                }
                            }

                            return (
                                <div key={rowKey} className={`chat__message ${rowClass}`}>

                                    {!isTool && msg.thinking && msg.thinking !== '' ? (
                                        <div className="chat__thinking_bubble">
                                            <div
                                                className="title"
                                                role="button"
                                                tabIndex={0}
                                                aria-expanded={m.expandThinking}
                                                onClick={toggleThinking}
                                                onKeyDown={(e: React.KeyboardEvent<HTMLDivElement>) => {
                                                    if (e.key === 'Enter' || e.key === ' ') {
                                                        e.preventDefault()
                                                        toggleThinking()
                                                    }
                                                }}
                                            >
                                                <div className="icon"><TipsAndUpdatesOutlined sx={{ fontSize: 20 }}/></div>
                                                <div className="text"><b>Thinking</b></div>
                                            </div>

                                            <div className={`body ${m.expandThinking ? 'body--visible' : 'body--hidden'}`}
                                                 aria-hidden={!m.expandThinking}>
                                                <div className="body__content">{msg.thinking}</div>
                                            </div>
                                        </div>
                                    ) : null}

                                    {isTool ? (
                                        <div className="chat__tool_bubble">
                                            <div
                                                className="title"
                                                role="button"
                                                tabIndex={0}
                                                aria-expanded={m.expandTool}
                                                onClick={toggleTool}
                                                onKeyDown={(e: React.KeyboardEvent<HTMLDivElement>) => {
                                                    if (e.key === 'Enter' || e.key === ' ') {
                                                        e.preventDefault()
                                                        toggleTool()
                                                    }
                                                }}
                                            >
                                                <div className="icon"><ConstructionOutlined sx={{ fontSize: 20 }}/></div>
                                                <div className="text">
                                                    <b>Tool</b>
                                                    {msg.toolName ? ` · ${msg.toolName}` : ''}
                                                </div>
                                            </div>

                                            <div className={`body ${m.expandTool ? 'body--visible' : 'body--hidden'}`}
                                                 aria-hidden={!m.expandTool}>
                                                <pre className="chat__tool_pre">{msg.content ?? ''}</pre>
                                            </div>
                                        </div>
                                    ) : (
                                        msg.content ? (
                                            <div className="chat__bubble">
                                                <ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeSanitize]}>
                                                    {msg.content}
                                                </ReactMarkdown>
                                            </div>
                                        ) : null
                                    )}

                                    {isLastInGroup && (
                                        <div className="chat__time">
                                            {(m.response.createdAt != null && !Number.isNaN(Date.parse(m.response.createdAt as any))
                                                ? new Date(m.response.createdAt as any)
                                                : new Date()
                                            ).toLocaleTimeString()}
                                        </div>
                                    )}
                                </div>
                            )
                        })}
                        <Loader visible={busy}/>
                    </div>
                </div>
            ) }


            <div className="chat__input_container">
                <form className="chat__form" onSubmit={handleSubmit}>
                    <textarea
                        value={input}
                        onChange={e => setInput(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="Ask Anything..."
                        aria-label="Message"
                        rows={2}
                    />
                    <Button variant="contained" type="submit" onClick={handleSubmit} disabled={!inputEnabled}>Submit</Button>
                </form>
            </div>
        </div>
    )
})

export default Chat
