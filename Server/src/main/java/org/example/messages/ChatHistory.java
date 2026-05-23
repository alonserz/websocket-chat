package org.example.messages;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatHistory {
    public ConcurrentLinkedQueue<ServerResponse> chatHistory;
    public String type;

    public ChatHistory(String type, ConcurrentLinkedQueue<ServerResponse> chatHistory) {
        this.type = type;
        this.chatHistory = chatHistory;
    }
}
