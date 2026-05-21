package org.example.messages;

public class ServerResponse {
    public final String type;
    public final String message;
    public final String username;
    public final long timestamp;
    public Static staticFiles;

    public ServerResponse(String type, String message, String username){
        this.type = type;
        this.message = message;
        this.username = username;
        this.timestamp = System.currentTimeMillis();
    }

    public ServerResponse(String type, String message, String username, Static staticFiles){
        this(type, message, username);
        this.staticFiles = staticFiles;
    }
}
