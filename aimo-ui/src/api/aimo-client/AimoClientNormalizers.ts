import type { ChatHistoryRequest, ChatResponse } from './AimoClientModel'

export function toDate(value: unknown): Date {
    if (value instanceof Date) return value
    if (typeof value === 'string' || typeof value === 'number') {
        const date = new Date(value)
        if (!Number.isNaN(date.getTime())) return date
    }
    return new Date(0)
}

export function normalizeChatResponse(raw: ChatResponse): ChatResponse {
    return {
        ...raw,
        createdAt: toDate((raw as { createdAt?: unknown }).createdAt),
    }
}

export function normalizeHistoryRequest(raw: ChatHistoryRequest): ChatHistoryRequest {
    return {
        ...raw,
        createdAt: toDate((raw as { createdAt?: unknown }).createdAt),
    }
}

