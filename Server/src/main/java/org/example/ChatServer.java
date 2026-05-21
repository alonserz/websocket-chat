package org.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

public class ChatServer {
    private final EventLoopGroup eventGroup = new NioEventLoopGroup();

    public ChannelFuture start(int port) {
        InetSocketAddress address = new InetSocketAddress(port);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(eventGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChatServerInitializer());
        ChannelFuture future = bootstrap.bind(address);
        future.syncUninterruptibly();
        return future;
    }

    public void destroy() {
        eventGroup.shutdownGracefully();
    }

    public static void main(String[] args) {
        final int port = 8080;

        final ChatServer endpoint = new ChatServer();
        ChannelFuture future = endpoint.start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(endpoint::destroy));

        future.channel().closeFuture().syncUninterruptibly();
    }
}
