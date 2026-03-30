import {chatClient, ChatSession} from "../chat-client/ChatClient";
import {HistoryEntry, HistoryService} from "./HistoryService";

export class HistoryServiceImpl implements HistoryService {

    private subscribers: Set<(items: HistoryEntry[]) => void> = new Set()
    private cachedHistory: HistoryEntry[] | null = null;

    async fetchHistory(): Promise<HistoryEntry[]> {
        const sessions = await chatClient.getChatSessions()
        const hist = sessions.map((session: ChatSession) => {
            return {
                id: session.chatId,
                title: session.title ? session.title : "New Chat",
            } as HistoryEntry
        })

        this.cachedHistory = hist
        this.emitUpdate(hist)

        return hist
    }

    async getHistory(): Promise<HistoryEntry[]> {
        if(this.cachedHistory) {
            return this.cachedHistory;
        } else {
            return this.fetchHistory()
        }
    }

    subscribe(sub: (items: HistoryEntry[]) => void): (() => void) {
        this.subscribers.add(sub)

        this.getHistory().then((history: HistoryEntry[]) => {
            // if still subscribed, trigger sub
            if (this.subscribers.has(sub)) {
                sub(history)
            }
        })

        return () => {
            this.subscribers.delete(sub)
        }
    }

    private emitUpdate(items: HistoryEntry[]) {
        this.subscribers.forEach((sub) => sub(items))
    }
}
