package org.example;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.example.messages.ServerResponse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ChatServerInitializer extends ChannelInitializer<Channel> {
    public static final ConcurrentLinkedQueue<ServerResponse> chatHistory = new ConcurrentLinkedQueue<>();
    public static final ConcurrentHashMap<String, String> fileLookupTable = new ConcurrentHashMap<>();

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
