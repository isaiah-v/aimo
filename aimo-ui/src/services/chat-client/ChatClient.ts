import {ChatClientImpl} from "./ChatClientImpl";

/**
 * Response returned when creating a new chat session.
 *
 * @interface NewChatResponse
 * @property {string} chatId - Unique identifier for the newly created chat session.
 */
export interface NewChatResponse {
    chatId: string
}

/**
 * Represents a request sent to the chat service.
 *
 * @interface ChatRequest
 * @property {string} message - The user-visible message or prompt to send to the chat backend.
 * @property {boolean} [stream] - When true, indicates the client expects streaming/incremental responses.
 */
export interface ChatRequest {
    message: string,
    stream?: boolean
}

/**
 * Named union type for roles that may originate a chat message.
 * Keeping this as a named export makes it easy to reuse across the codebase.
 *
 * @typedef {'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'} Role
 */
export type Role = 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'

/**
 * Represents a single message or event in a chat conversation.
 *
 * @interface ChatMessage
 * @property {number} id - Monotonically increasing identifier for the message/event.
 * @property {Role} role - Originator of the message (user, assistant, system, or tool).
 * @property {string} [response] - Finalized response text for this message when available.
 * @property {string} [thinking] - Partial/in-progress content (used when streaming incremental updates).
 * @property {number} [timestamp] - Unix epoch milliseconds when the message/event was created.
 * @property {boolean} done - True when this message is complete/final (no more incremental updates expected).
 */
export interface ChatMessage {
    id: number
    role: Role
    response?: string
    thinking?: string
    timestamp?: number
    done: boolean
}

export interface ChatSession {
    chatId: string
    title?: string
}

export interface ChatSessionUpdateRequest {
    title: string
}


/**
 * Callback container used for streaming or incremental delivery from the chat client.
 *
 * Implementers should handle incoming partial or final ChatMessage objects in `onMessage`.
 *
 * @interface Callback
 * @property {(message: ChatMessage) => void} onMessage - Invoked for each message or incremental chunk produced by the server.
 */
export interface Callback {
    onMessage: (message: ChatMessage) => void
}

/**
 * High-level client interface for interacting with the chat backend.
 *
 * Implementations should provide the following methods:
 *  - newChat: create a new chat session and return its id
 *  - chat: send a message/request to an existing chat session (optionally stream results via Callback)
 *  - history: retrieve the message history for a chat session
 *
 * @interface ChatClient
 * @property {() => Promise<NewChatResponse>} createChat - Create a new chat session. Resolves with NewChatResponse containing chatId.
 * @property {(chatId: string, request: ChatRequest, callback?: Callback) => Promise<ChatMessage>} chat - Send a ChatRequest and optionally receive streaming updates via callback.
 * @property {(chatId: string) => Promise<ChatMessage[]>} getChatHistory - Fetch the message history for chatId.
 */
export interface ChatClient {
    chat: (chatId: string, request: ChatRequest, callback?: Callback) => Promise<ChatMessage>
    getChatHistory: (chatId: string) => Promise<ChatMessage[]>
    createChatSession: () => Promise<NewChatResponse>
    getChatSessions: () => Promise<ChatSession[]>
    updateChatSession: (chatId: string, request: ChatSessionUpdateRequest) => Promise<void>
    deleteChatSession: (chatId: string) => Promise<void>
}

// TODO make the baseUrl configurable
/**
 * Instance of the ChatClient for use throughout the application.
 */
export const chatClient: ChatClient = new ChatClientImpl('http://localhost:8080')