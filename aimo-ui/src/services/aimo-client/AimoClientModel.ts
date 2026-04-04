export interface ChatRequest {
    prompt: string,
    stream: boolean,
}

export interface ChatResponse {
    chatId: string,
    responseId: number,
    messages: ChatMessage[],
}

export type MessageType = 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL';

export interface ChatMessage {
    messageId: number,
    type: MessageType,
    content?: string,
    thinking?: string,
    toolName?: string,
    createdAt?: string,
    /** Exchange round from the API (history request id / stream response id). With messageId + type forms a stable row key (USER and ASSISTANT often share messageId within one request). */
    requestId?: number,
}

export interface ChatHistoryRequest {
    chatId: string,
    requestId: number,
    messages: ChatMessage[],
}

export interface NewChatResponse {
    chatId: string,
}

export interface ChatSession {
    chatId: string,
    title?: string,
}

export interface ChatSessionUpdateRequest {
    title: string,
}

export interface ChatCallback {
    onMessage: (response: ChatResponse) => void,
}
