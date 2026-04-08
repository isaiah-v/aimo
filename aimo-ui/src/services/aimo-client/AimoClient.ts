import {
    ChatCallback,
    ChatHistoryRequest,
    ChatRequest,
    ChatResponse,
    ChatSession, ChatSessionUpdateRequest,
    NewChatResponse
} from "./AimoClientModel";
const CONTROLLER_CHAT = "/aimo-api/chat"
const CONTROLLER_HISTORY = "/aimo-api/history"
const CONTROLLER_SESSION = "/aimo-api/session"

export interface AimoClient {
    chat: (chatId: string, request: ChatRequest, callback: ChatCallback) => Promise<ChatResponse | null>
    getHistory: (chatId: string) => Promise<ChatHistoryRequest[]>
    createChatSession: () => Promise<NewChatResponse>
    getChatSessions: () => Promise<ChatSession[]>
    updateChatSession: (chatId: string, request: ChatSessionUpdateRequest) => Promise<void>
    deleteChatSession: (chatId: string) => Promise<void>
}

class AimoClientImpl implements AimoClient {

    private readonly baseUrl: string;

    constructor(baseUrl: string) {
        // remove trailing slash(es) if present
        this.baseUrl = baseUrl.replace(/\/+$/, '');
    }

    private toDate(value: unknown): Date {
        if (value instanceof Date) return value
        if (typeof value === 'string' || typeof value === 'number') {
            const d = new Date(value)
            if (!Number.isNaN(d.getTime())) return d
        }
        return new Date(0)
    }

    private normalizeChatResponse(raw: ChatResponse): ChatResponse {
        return {
            ...raw,
            createdAt: this.toDate((raw as { createdAt?: unknown }).createdAt),
        }
    }

    private normalizeHistoryRequest(raw: ChatHistoryRequest): ChatHistoryRequest {
        return {
            ...raw,
            createdAt: this.toDate((raw as { createdAt?: unknown }).createdAt),
        }
    }

    chat = (
        chatId: string,
        request: ChatRequest,
        callback: ChatCallback
    ) => this.POST(CONTROLLER_CHAT, `/${chatId}`, { 'Content-Type': 'application/json' }, request).then(async res => {
        if (!res.body) {
            // No stream support; try to parse whole body as JSON
            const txt = await res.text()

            const parsed = JSON.parse(txt)

            return this.normalizeChatResponse(parsed as ChatResponse)
        }

        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let lastEvent: ChatResponse | null = null;

        const emitJson = (jsonStr: string) => {
            if (!jsonStr) return
            try {
                const response = this.normalizeChatResponse(JSON.parse(jsonStr) as ChatResponse)

                lastEvent = response
                callback?.onMessage(response)
            } catch {
                // TODO error
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

    getHistory = (
        chatId: string
    ) => this.GET(CONTROLLER_HISTORY, `/${chatId}`).then(async res => {
        if (!res.ok) {
            throw new Error(`failed to fetch chat history: ${res.status} ${res.statusText}`)
        }

        const txt = await res.text()
        const parsed = JSON.parse(txt) as ChatHistoryRequest[]

        return parsed.map((req) => this.normalizeHistoryRequest(req))
    })

    createChatSession = () => this.POST(CONTROLLER_SESSION, "/").then(async res => {
        if(!res.ok) {
            throw new Error(`failed to create new session: ${res.status} ${res.statusText}`)
        }

        const txt = await res.text()
        const parsed = JSON.parse(txt)

        return parsed as NewChatResponse
    })

    deleteChatSession = (
        chatId: string
    ) => this.DELETE(CONTROLLER_SESSION, `/${chatId}`).then(async res => {
        if(!res.ok) {
            throw new Error(`failed to delete session: ${res.status} ${res.statusText}`)
        }
    })

    getChatSessions = () => this.GET(CONTROLLER_SESSION, "/").then(async res => {
        if(!res.ok) {
            throw new Error(`failed to get session: ${res.status} ${res.statusText}`)
        }

        const txt = await res.text()
        const parsed = JSON.parse(txt)

        return parsed as ChatSession[]
    })

    updateChatSession = (
        chatId: string,
        request: ChatSessionUpdateRequest
    ) => this.POST(CONTROLLER_SESSION, `/${chatId}`, undefined, request).then(async res => {
        if (!res.ok) {
            throw new Error(`failed to update session: ${res.status} ${res.statusText}`)
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

export const aimoClient: AimoClient = new AimoClientImpl('http://localhost:8080')
