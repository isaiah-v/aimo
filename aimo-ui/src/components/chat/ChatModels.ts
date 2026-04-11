import type {ChatMessage, ChatResponse} from "../../api/aimo-client/AimoClientModel";

export interface Message {
    response: ChatResponse
    message: ChatMessage
    expandThinking: boolean
    expandTool: boolean
}
