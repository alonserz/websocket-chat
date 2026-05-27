package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.example.messages.ServerResponse;
import org.example.router.EndpointRouter;
import org.example.router.FileRequestHandler;
import org.example.router.FileUploadHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ChatServerInitializer extends ChannelInitializer<Channel> {
    public static final ConcurrentLinkedQueue<ServerResponse> chatHistory = new ConcurrentLinkedQueue<>();
    public static final ConcurrentHashMap<String, String> fileLookupTable = new ConcurrentHashMap<>();
    public static final ObjectMapper objectMapper = new ObjectMapper();
    public static final EndpointRouter router = new EndpointRouter();

    public ChatServerInitializer() {
        ChatServerInitializer.router.add("upload", new FileUploadHandler());
        ChatServerInitializer.router.add("default", new FileRequestHandler());
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(50 * 1024 * 1024));
        String websocketURI = "/ws";
        pipeline.addLast(new HttpRequestHandler(websocketURI));
        pipeline.addLast(new WebSocketServerProtocolHandler(websocketURI, null, true, 50 * 1024 * 1024));
        pipeline.addLast(new TextWebSocketFrameHandler());
    }
}
