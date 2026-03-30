import {Alert, AlertLevel, AlertService} from "./AlertService";
import {UUID} from "../../utils/UUID";

const timeout = 4000;

export class AlertServiceImpl implements AlertService {

    alerts: Alert[] = [];
    subscribeCallbacks: Set<(alerts: Alert[]) => void> = new Set();

    alert(level: AlertLevel, message: string): void {
        const alert = {
            id: UUID.createUUID(),
            level: level,
            message: message,
        }

        this.alerts.push(alert);

        setTimeout(() => {
            // remove alert from list
            this.alerts = this.alerts.filter((a) => a.id !== alert.id)
            this.emitUpdate()
        }, timeout);

        this.emitUpdate()
    }

    success(message: string): void {
        this.alert("success", message)
    }
    info(message: string): void {
        this.alert("info", message)
    }
    warning(message: string): void {
        this.alert("warning", message)
    }
    error(message: string): void {
        this.alert("error", message)
    }

    subscribe(callback: (alerts: Alert[]) => void): () => void {
        this.subscribeCallbacks.add(callback)

        // Initial callback
        callback(this.alerts.map((a) => a))

        return () => {
            this.subscribeCallbacks.delete(callback)
        }
    }

    private emitUpdate(): void {
         this.subscribeCallbacks.forEach((cb) => {
            cb(this.alerts.map((a) => a))
        })
    }
}