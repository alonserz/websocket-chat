package org.example.router;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.io.IOException;

public interface IHandler {
    void run(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException;
}
