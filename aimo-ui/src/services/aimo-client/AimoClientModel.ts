export interface ChatRequest {
    prompt: string,
    stream: boolean,
}

export interface ChatResponse {
    chatId: string,
    responseId: string,
    messages: ChatMessage[],
    createdAt: Date,
}

export type MessageType = 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL';

export interface ChatMessage {
    messageId: number,
    type: MessageType,
    content?: string,
    thinking?: string,
    toolName?: string,
}

export interface ChatHistoryRequest {
    chatId: string,
    requestId: string,
    messages: ChatMessage[],
    createdAt: Date,
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
