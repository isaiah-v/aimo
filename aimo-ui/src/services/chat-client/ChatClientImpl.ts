import {Callback, ChatClient, ChatRequest, ChatMessage, NewChatResponse, ChatSession, ChatSessionUpdateRequest} from "./ChatClient";
import { alertService } from "../alert-service/AlertService";

const CHAT = "/chat"
const CHAT_SESSION = "/chat-session"

export class ChatClientImpl implements ChatClient {
    private readonly baseUrl: string;

    constructor(baseUrl: string) {
        // remove trailing slash(es) if present
        this.baseUrl = baseUrl.replace(/\/+$/, '');
    }

    chat = (
        chatId: string,
        request: ChatRequest,
        callback?: Callback
    ) => this.POST(CHAT, `/${chatId}`, { 'Content-Type': 'application/json' }, request).then(async res => {
        if (!res.body) {
            // No stream support; try to parse whole body as JSON
            const txt = await res.text()

            const parsed = JSON.parse(txt)

            return parsed as ChatMessage
        }

        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let lastEvent: ChatMessage = null;

        const emitJson = (jsonStr: string) => {
            if (!jsonStr) return
            try {
                const response = JSON.parse(jsonStr) as ChatMessage

                lastEvent = response
                callback?.onMessage(response)
            } catch {
                // TODO add an error callback?
                // If it isn't a structured MessageEvent, send raw string
                const ev: ChatMessage = { id: -1, role: "SYSTEM", response: jsonStr, thinking: '', done: true, timestamp: Date.now()}

                lastEvent = ev
                callback?.onMessage(ev)
            }
        }

        // Read stream chunks and attempt two strategies:
        // 1) NDJSON / newline-delimited objects
        // 2) Concatenated JSON objects using brace counting with basic string/escape handling
        let done = false
        while (!done) {
            const { value, done: streamDone } = await reader.read()
            done = !!streamDone
            buffer += value ? decoder.decode(value, { stream: true }) : ''

            // Handle newline-delimited objects first
            let nlIndex: number
            while ((nlIndex = buffer.indexOf('\n')) >= 0) {
                const line = buffer.slice(0, nlIndex).trim()
                buffer = buffer.slice(nlIndex + 1)
                if (line) emitJson(line)
            }

            // Attempt to extract concatenated objects from remaining buffer
            // Basic state machine to handle strings and escapes so braces inside strings don't break parsing
            let braceDepth = 0
            let inString = false
            let escape = false
            let start = -1
            for (let i = 0; i < buffer.length; i++) {
                const ch = buffer[i]
                if (escape) {
                    escape = false
                    continue
                }
                if (ch === '\\') {
                    escape = true
                    continue
                }
                if (ch === '"' ) {
                    inString = !inString
                    continue
                }
                if (inString) continue
                if (ch === '{') {
                    if (braceDepth === 0) start = i
                    braceDepth++
                } else if (ch === '}') {
                    braceDepth--
                    if (braceDepth === 0 && start >= 0) {
                        const objStr = buffer.slice(start, i + 1)
                        emitJson(objStr)
                        buffer = buffer.slice(i + 1)
                        // reset scanner to beginning of new buffer
                        i = -1
                        start = -1
                    }
                }
            }
        }

        // After stream end, try to parse any leftover content
        const leftover = buffer.trim()
        if (leftover) {
            // Try newline split first, then fallback to single JSON parse
            const parts = leftover.split('\n').map(p => p.trim()).filter(Boolean)
            if (parts.length > 1) {
                parts.forEach(emitJson)
            } else {
                emitJson(leftover)
            }
        }

        return lastEvent
    })

    getChatHistory = (
        chatId: string
    ) => this.GET(CHAT, `/${chatId}`).then(async res => {
        if(!res.ok) {
            let msg: string = ''
            if(res.status === 404) {
                // no history yet
                msg = `Chat session not found: ${chatId}`
            } else {
                msg = `Failed to fetch chat history: ${res.status} ${res.statusText}`
            }
            alertService.error(msg)
            throw new Error(msg)
        }

        const txt = await res.text()
        const parsed = JSON.parse(txt)
        return parsed as ChatMessage[]
    })

    createChatSession = () => this.POST(CHAT_SESSION, "/").then(async res => {
        if(!res.ok) {
            throw new Error(`Failed to create new chat: ${res.status} ${res.statusText}`)
        }

        const txt = await res.text()
        const parsed = JSON.parse(txt)

        return parsed as NewChatResponse
    })

    getChatSessions = () => this.GET(CHAT_SESSION, "/").then(async res => {
        const txt = await res.text()
        const parsed = JSON.parse(txt)

        return parsed as ChatSession[]
    })

    updateChatSession = (
        chatId: string,
        request: ChatSessionUpdateRequest
    ) => this.POST(CHAT_SESSION, `/${chatId}`, undefined, request).then(async res => {
        if(!res.ok) {
            throw new Error(`Failed to fetch chat history: ${res.status} ${res.statusText}`)
        }
    })
    deleteChatSession = (
        chatId: string
    ) => this.DELETE(CHAT_SESSION, `/${chatId}`).then(async res => {
        if(!res.ok) {
            throw new Error(`Failed to fetch chat history: ${res.status} ${res.statusText}`)
        }
    })


    private createUrl(controller: string, path: string): string {
        return `${this.baseUrl}${controller}${path}`;
    }

    private async POST (
        controller: string,
        path: string,
        headers?: HeadersInit,
        body?: any
    ): Promise<Response> {
        return this.request('POST', controller, path, headers, JSON.stringify(body))
    }

    private async GET (
        controller: string,
        path: string,
        headers?: HeadersInit
    ): Promise<Response> {
        return this.request('GET', controller, path, headers)
    }

    private async DELETE (
        controller: string,
        path: string,
        headers?: HeadersInit
    ): Promise<Response> {
        return this.request('DELETE', controller, path, headers)
    }

    private async request (
        method: string,
        controller: string,
        path: string,
        headers?: HeadersInit,
        body?: BodyInit
    ): Promise<Response> {
        const url = this.createUrl(controller, path)
        return fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json', ...headers },
            body: body
        })
    }
}
