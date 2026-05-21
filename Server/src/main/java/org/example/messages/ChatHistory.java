package org.example.messages;
import java.util.List;

public class ChatHistory {
    public List<ServerResponse> chatHistory;
    public String type;

    public ChatHistory(String type, List<ServerResponse> chatHistory){
        this.type = type;
        this.chatHistory = chatHistory;
    }
}
