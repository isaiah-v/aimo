import type {ChatMessage} from "../../services/aimo-client/AimoClientModel";

export interface Message {
    message: ChatMessage
    expandThinking: boolean
    expandTool: boolean
}
