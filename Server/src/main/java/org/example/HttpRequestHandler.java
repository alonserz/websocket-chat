package org.example;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.io.*;
import java.util.*;

import static org.example.ChatServerInitializer.router;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String websocketURI;

    public HttpRequestHandler(String websocketURI) {
        this.websocketURI = websocketURI;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {
        if (websocketURI.equalsIgnoreCase(request.uri())) {
            ctx.fireChannelRead(request.retain());
        } else {
            router.handle(ctx, request);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}