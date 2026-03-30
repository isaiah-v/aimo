import { AlertServiceImpl } from "./AlertServiceImpl"

export interface Alert {
    id?: string
    message: string
    level: AlertLevel
}

export type AlertLevel = "success" | "info" | "warning" | "error"

export interface AlertService {
    alert(level: AlertLevel, message: string): void;
    success(message: string): void;
    info(message: string): void;
    warning(message: string): void;
    error(message: string): void;
    subscribe(callback: (alerts: Alert[]) => void): () => void;
}

export const alertService: AlertService = new AlertServiceImpl()