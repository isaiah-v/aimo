import {aimoClient} from "../../api/aimo-client/AimoClient";
import type {ChatSession} from "../../api/aimo-client/AimoClientModel";
import {aimoUiClient} from "../../api/aimo-ui-client/AimoUiClient";
import {HistoryEntry, HistoryService} from "./HistoryService";

export class HistoryServiceImpl implements HistoryService {

    private subscribers: Set<(items: HistoryEntry[]) => void> = new Set()
    private cachedHistory: HistoryEntry[] | null = null;

    async fetchHistory(): Promise<HistoryEntry[]> {
        const [sessions, titles] = await Promise.all([
            aimoClient.getChatSessions(),
            aimoUiClient.getTitles()
        ])
        const titleByChatId = new Map(titles.map((sessionTitle) => [sessionTitle.chatId, sessionTitle.title]))

        const hist = sessions.map((session: ChatSession) => {
            const title = titleByChatId.get(session.chatId)

            return {
                id: session.chatId,
                title: title ? title : "New Chat",
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
