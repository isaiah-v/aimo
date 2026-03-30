import { ChatSessionImpl } from './ChatSessionImpl.js'

export interface ChatSession {
    get id(): string | null
    setId(id?: string, push?: boolean): Promise<string>
    clear(push?: boolean): Promise<void>
    onChange(cb: (id: string | null) => Promise<void>): () => void
}

export const chatSession: ChatSession = new ChatSessionImpl()