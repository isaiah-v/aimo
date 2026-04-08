import type {ChatMessage, ChatResponse} from "../../services/aimo-client/AimoClientModel";

export interface Message {
    response: ChatResponse
    message: ChatMessage
    expandThinking: boolean
    expandTool: boolean
}
