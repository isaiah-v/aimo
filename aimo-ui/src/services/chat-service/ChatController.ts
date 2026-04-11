import {aimoClient} from '../../api/aimo-client/AimoClient'
import type {ChatHistoryRequest, ChatMessage, ChatResponse} from '../../api/aimo-client/AimoClientModel'
import type {ChatHandle} from '../../components/chat/Chat'
import React from "react";
import {chatSession} from "../chat-session-service/ChatSession";
import { historyService } from "../history-service/HistoryService";

function flattenHistory(requests: ChatHistoryRequest[]): ChatResponse[] {
    const sorted = [...requests].sort((a, b) => {
        const aTime = a.createdAt.getTime()
        const bTime = b.createdAt.getTime()
        return aTime - bTime
    })
    return sorted.map((r) => ({
        chatId: r.chatId,
        responseId: r.requestId,
        messages: r.messages,
        createdAt: r.createdAt,
    }))
}

function lastMessageFromResponse(resp: ChatResponse | null): ChatMessage | undefined {
    if (!resp?.messages?.length) return undefined
    return resp.messages[resp.messages.length - 1]
}

/**
 * Spring AI often sends growing `content`; some stacks send only the new fragment.
 */
function streamTextDeltas(
    acc: { content: string; thinking: string },
    msg: ChatMessage
): { deltaContent: string; deltaThinking: string } {
    const content = msg.content ?? ''
    const thinking = msg.thinking ?? ''
    let deltaContent: string
    if (content.startsWith(acc.content)) {
        deltaContent = content.slice(acc.content.length)
        acc.content = content
    } else {
        deltaContent = content
        acc.content += content
    }
    let deltaThinking: string
    if (thinking.startsWith(acc.thinking)) {
        deltaThinking = thinking.slice(acc.thinking.length)
        acc.thinking = thinking
    } else {
        deltaThinking = thinking
        acc.thinking += thinking
    }
    return { deltaContent, deltaThinking }
}

function streamAccKey(responseId: string, msg: ChatMessage): string {
    return `${responseId}:${msg.messageId}:${msg.type}`
}

export class ChatController {
    private chatHandle?: React.RefObject<ChatHandle | null> | null

    private unsubscribeSessionChange: (() => void) | null = null

    attach(chatHandle: React.RefObject<ChatHandle | null>) {
        if (this.chatHandle) {
            return
        }
        if (this.unsubscribeSessionChange) {
            this.unsubscribeSessionChange()
        }

        this.chatHandle = chatHandle
        this.unsubscribeSessionChange = chatSession.onChange(async (id: string | null) => {
            if (!id) {
                this.chatHandle?.current?.setResponses([])
            } else {
                const enableInput = this.chatHandle?.current?.disableInput()
                try {
                    const history = await aimoClient.getHistory(id)
                    const responses = flattenHistory(history)
                    this.chatHandle?.current?.setResponses(responses)

                    void historyService.fetchHistory()
                } catch (error) {
                    void chatSession.clear(false)
                    throw error
                } finally {
                    enableInput?.()
                }
            }
        })
    }

    detach() {
        this.chatHandle = undefined

        if (this.unsubscribeSessionChange) {
            this.unsubscribeSessionChange()
            this.unsubscribeSessionChange = null
        }
    }

    /**
     * Pass this method to the Chat component as `onSend`.
     * It:
     * - inserts an assistant placeholder message,
     * - calls the API with `stream: true`,
     * - appends streamed chunks to the placeholder via the Chat imperative API.
     */
    onSend = async (userMsg: ChatMessage): Promise<ChatMessage | undefined> => {
        let id = chatSession.id
        if (!id) {
            const newChat = await aimoClient.createChatSession()

            // TODO: I need all listeners to finish before proceeding
            id = await chatSession.setId(newChat.chatId)
            // Create a response wrapper for the user message
            const userResponse: ChatResponse = {
                chatId: id,
                responseId: `local-${Date.now()}`,
                messages: [userMsg],
                createdAt: new Date(),
            }
            this.chatHandle?.current?.addResponse(userResponse)
        }

        const handle = this.chatHandle?.current
        if (!handle) {
            throw new Error('Chat handle is not attached. Cannot send message.')
        }

        const req = { prompt: userMsg.content ?? '', stream: true as const }
        const streamAccByRequestAndMessage = new Map<string, { content: string; thinking: string }>()
        const unsetBusy = handle.busy()
        const enableInput = handle.disableInput()
        try {
            return await aimoClient.chat(id, req, {
                onMessage: (resp: ChatResponse) => {
                    const list = resp.messages
                    if (!list?.length) return

                    unsetBusy()

                    const responseId = resp.responseId
                    for (const apiMsg of list) {
                        const accKey = streamAccKey(responseId, apiMsg)
                        let acc = streamAccByRequestAndMessage.get(accKey)
                        if (!acc) {
                            acc = { content: '', thinking: '' }
                            streamAccByRequestAndMessage.set(accKey, acc)
                            handle.addResponse(resp)
                            acc.content = apiMsg.content ?? ''
                            acc.thinking = apiMsg.thinking ?? ''
                        } else {
                            const { deltaContent, deltaThinking } = streamTextDeltas(acc, apiMsg)
                            if (deltaContent || deltaThinking) {
                                handle.appendMessage(resp, {
                                    messageId: apiMsg.messageId,
                                    type: apiMsg.type,
                                    content: deltaContent,
                                    thinking: deltaThinking,
                                    toolName: apiMsg.toolName
                                })
                            }
                        }
                    }
                },
            }).then((resp) => lastMessageFromResponse(resp))
        } catch (err) {
            const errText = typeof err === 'string' ? err : (err instanceof Error ? err.message : 'Error')
            try {
                const errorResponse: ChatResponse = {
                    chatId: id,
                    responseId: `error-${Date.now()}`,
                    messages: [{
                        messageId: Date.now(),
                        type: 'SYSTEM',
                        content: `[Error] ${errText}`,
                    }],
                    createdAt: new Date(),
                }
                handle.addResponse(errorResponse)
            } catch {
                // ignore
            }
            throw err
        } finally {
            unsetBusy()
            enableInput()
        }
    }
}
