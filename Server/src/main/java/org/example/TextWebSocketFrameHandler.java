package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.example.messages.*;
import org.example.user.User;
import org.example.user.UserStates;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final HashMap<String, String> fileLookupTable;
    private final List<ServerResponse> chatHistory;
    private final User user = new User();
    private Channel ctxChannel;

    public TextWebSocketFrameHandler(List<ServerResponse> chatHistory, HashMap<String, String> fileLookupTable) {
        this.chatHistory = chatHistory;
        this.fileLookupTable = fileLookupTable;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            channelGroup.add(ctx.channel());
        } else {
            super.userEventTriggered(ctx, event);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctxChannel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        ClientRequest message = objectMapper.readValue(msg.text(), ClientRequest.class);
        switch (message.type) {
            case "connect":
                ClientConnectRequest connectMessage = objectMapper.readValue(msg.text(), ClientConnectRequest.class);
                userConnect(connectMessage);
                break;
            case "userMessage":
                userMessage(message);
                break;
            case "disconnect":
                userDisconnect(ctx);
                break;
            default:
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void broadcast(String message) {
        for (Channel channel : channelGroup) {
            channel.writeAndFlush(new TextWebSocketFrame(message));
        }
    }

    private void userConnect(ClientConnectRequest message) throws JsonProcessingException {
        if (user.state == UserStates.CONNECTED) {
            return;
        }
        user.updateUsername(message.username);
        user.updateState(UserStates.CONNECTED);
        ServerResponse serverResponse = new ServerResponse("systemMessage", "User " + user.username + " connected to room!", "System");
        String serverResponseJSON = objectMapper.writeValueAsString(serverResponse);
        sendHistory();
        broadcast(serverResponseJSON);
        addResponseToHistory(serverResponse);
    }

    private void userMessage(ClientRequest message) throws JsonProcessingException {
        ServerResponse serverResponse;
        if (message.uuid != null) {
            serverResponse = new ServerResponse("userMessage", message.message, user.username, new Static("image", message.uuid, "25.24.84.130:8080"));
        } else {
            serverResponse = new ServerResponse("userMessage", message.message, user.username);
        }
        String serverResponseJSON = objectMapper.writeValueAsString(serverResponse);
        broadcast(serverResponseJSON);
        addResponseToHistory(serverResponse);
    }

    private void userDisconnect(ChannelHandlerContext ctx) throws JsonProcessingException {
        user.updateState(UserStates.DISCONNECTED);
        ServerResponse serverResponse = new ServerResponse("systemMessage", "User " + user.username + " disconnected from room!", "System");
        String serverResponseJSON = objectMapper.writeValueAsString(serverResponse);
        broadcast(serverResponseJSON);
        channelGroup.remove(ctx.channel());
        addResponseToHistory(serverResponse);
    }

    private void addResponseToHistory(ServerResponse response) {
        chatHistory.add(response);
    }

    private void sendHistory() throws JsonProcessingException {
        ChatHistory serverResponseHistory = new ChatHistory("chatHistory", chatHistory);
        String chatHistoryJSON = objectMapper.writeValueAsString(serverResponseHistory);
        ctxChannel.writeAndFlush(new TextWebSocketFrame(chatHistoryJSON));
    }
}