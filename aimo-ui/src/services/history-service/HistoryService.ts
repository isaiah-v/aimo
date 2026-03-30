import { HistoryServiceImpl } from "./HistoryServiceImpl";

export interface HistoryEntry {
    id: string;
    title: string;
}

export interface HistoryService {
    fetchHistory(): Promise<HistoryEntry[]>
    getHistory(): Promise<HistoryEntry[]>
    subscribe(listener: (items: HistoryEntry[]) => void): (() => void)
}

export const historyService: HistoryService = new HistoryServiceImpl();
