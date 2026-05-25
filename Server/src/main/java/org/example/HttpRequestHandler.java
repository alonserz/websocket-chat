package org.example;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.example.router.EndpointRouter;
import org.example.router.FileRequestHandler;
import org.example.router.FileUploadHandler;

import java.io.*;
import java.util.*;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String websocketURI;
    private final EndpointRouter router = new EndpointRouter();

    public HttpRequestHandler(String websocketURI) {
        this.websocketURI = websocketURI;
    }

    public void handlerAdded(ChannelHandlerContext ctx) {
        router.add("upload", new FileUploadHandler());
        router.add("default", new FileRequestHandler());
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