import {ChatMessage, ChatRequest, chatClient} from '../chat-client/ChatClient'
import type {ChatHandle} from '../../components/chat/Chat'
import React from "react";
import {chatSession} from "../chat-session-service/ChatSession";
import { historyService } from "../history-service/HistoryService";

export class ChatController {
    private chatHandle?: React.RefObject<ChatHandle> | null

    private unsubscribeSessionChange: () => void | null = null


    attach(chatHandle: React.RefObject<ChatHandle>) {
        if(this.chatHandle) {
            return
        }
        if(this.unsubscribeSessionChange) {
            this.unsubscribeSessionChange()
        }

        this.chatHandle = chatHandle
        this.unsubscribeSessionChange = chatSession.onChange(async (id: string | null) => {
            if (!id) {
                this.chatHandle?.current?.setMessages([])
            } else {
                const enableInput = this.chatHandle?.current?.disableInput()
                try {
                    const messages = await chatClient.getChatHistory(id)
                    this.chatHandle?.current?.setMessages(messages)

                    void historyService.fetchHistory()
                } catch (error) {
                    void chatSession.clear(false)
                    throw error
                } finally {
                    enableInput()
                }
            }
        })
    }

    detach() {
        this.chatHandle = undefined

        if(this.unsubscribeSessionChange) {
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
        if(!id) {
            const newChat = await chatClient.createChatSession()

            // TODO: I need all listeners to finish before proceeding
            id = await chatSession.setId(newChat.chatId)
            this.chatHandle?.current?.addMessage(userMsg)
        }

        const handle = this.chatHandle?.current
        if (!handle) {
            // no UI attached; still call API but cannot update UI
            const reqFallback: ChatRequest = {message: userMsg.response, stream: false}

            const unsetBusy = this.chatHandle?.current?.busy()
            const enableInput = this.chatHandle?.current?.disableInput()
            try {
                return await chatClient.chat(id, reqFallback)
            } catch {
                return undefined
            } finally {
                unsetBusy()
                enableInput()
            }
        }

        const req: ChatRequest = {message: userMsg.response, stream: true}
        let isFirstChunk = true
        const unsetBusy = this.chatHandle?.current?.busy()
        const enableInput = this.chatHandle?.current?.disableInput()
        try {
            // always append incoming chunks to the placeholder message we created above
            return await chatClient.chat(id, req, {
                onMessage: (ev: ChatMessage) => {
                    unsetBusy()

                    if (isFirstChunk) {
                        isFirstChunk = false
                        handle.addMessage(ev)
                    } else {
                        handle.appendMessage(ev)
                    }
                }
            })
        } catch (err) {
            // show error text in the assistant message
            const errText = typeof err === 'string' ? err : (err instanceof Error ? err.message : 'Error')
            try {
                handle.appendMessage({
                    id: Date.now(),
                    response: `\n\n[Error] ${errText}`,
                    role: 'SYSTEM',
                    timestamp: Date.now(),
                    done: true
                })
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