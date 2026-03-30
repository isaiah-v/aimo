import {ChatMessage} from "../../services/chat-client/ChatClient";

export interface Message {
    message: ChatMessage
    expandThinking: boolean
}