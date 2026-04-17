import {
    ChatCallback,
    ChatHistoryRequest,
    ChatRequest,
    ChatResponse,
    ChatSession
} from "./AimoClientModel";
import { normalizeChatResponse, normalizeHistoryRequest } from "./AimoClientNormalizers";
import {ApiClient} from "../api-client/ApiClient";
import {ResponseBuilder} from "./ResponseBuilder";

const CONTROLLER_CHAT = "/aimo-api/chat"
const CONTROLLER_HISTORY = "/aimo-api/history"
const CONTROLLER_SESSION = "/aimo-api/session"

export interface AimoClient {
    chat: (chatId: string, request: ChatRequest, callback: ChatCallback) => Promise<ChatResponse | null>
    getHistory: (chatId: string) => Promise<ChatHistoryRequest[]>
    createChatSession: () => Promise<ChatSession>
    getChatSessions: () => Promise<ChatSession[]>
    deleteChatSession: (chatId: string) => Promise<void>
}

class AimoClientImpl extends ApiClient implements AimoClient {

    constructor(baseUrl: string) {
        // remove trailing slash(es) if present
        super(baseUrl)
    }

    chat = (
        chatId: string,
        request: ChatRequest,
        callback: ChatCallback
    ) => this.POST(
        CONTROLLER_CHAT,
        `/${encodeURIComponent(chatId)}`,
        {
            'Content-Type': 'application/json',
            'X-Timezone-Offset': String(new Date().getTimezoneOffset()),
        },
        request,
    ).then(async res => {
        if (!res.body) {
            // No stream support; try to parse whole body as JSON
            const txt = await res.text()
            return normalizeChatResponse(JSON.parse(txt) as ChatResponse)
        }

        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        const builder = new ResponseBuilder(
            (response) => callback?.onResponseChunk?.(response),
            (response) => callback?.onMessageComplete?.(response),
        )

        let done = false
        while (!done) {
            const { value, done: streamDone } = await reader.read()
            done = streamDone
            if (value) {
                const chunk = decoder.decode(value, { stream: true })
                builder.push(chunk)
            }
        }

        builder.flush()
        if (builder.last) {
            callback?.onComplete?.(builder.last)
        }
        return builder.last
    })

    getHistory = (
        chatId: string
    ) => this.GET(CONTROLLER_HISTORY, `/${encodeURIComponent(chatId)}`).then(async res => {
        if (!res.ok) {
            throw new Error(`failed to fetch chat history: ${res.status} ${res.statusText}`)
        }

        const txt = await res.text()
        const parsed = JSON.parse(txt) as ChatHistoryRequest[]

        return parsed.map((req) => normalizeHistoryRequest(req))
    })

    createChatSession = () => this.POST(CONTROLLER_SESSION, "/").then(async res => {
        if(!res.ok) {
            throw new Error(`failed to create new session: ${res.status} ${res.statusText}`)
        }

        const txt = await res.text()
        const parsed = JSON.parse(txt)

        return parsed as ChatSession
    })

    deleteChatSession = (
        chatId: string
    ) => this.DELETE(CONTROLLER_SESSION, `/${encodeURIComponent(chatId)}`).then(async res => {
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

}

export const aimoClient: AimoClient = new AimoClientImpl('http://localhost:8080')
