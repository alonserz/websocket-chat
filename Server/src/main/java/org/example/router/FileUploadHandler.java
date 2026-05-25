package org.example.router;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.example.messages.ImageUpload;

import java.io.FileOutputStream;
import java.io.IOException;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.example.ChatServerInitializer.fileLookupTable;
import static org.example.ChatServerInitializer.objectMapper;

public class FileUploadHandler implements IHandler {
    private FullHttpRequest request;
    private ChannelHandlerContext ctx;

    private void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpResponse response) {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        HttpUtil.setContentLength(response, response.content().readableBytes());
        if (!keepAlive) {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ChannelFuture flushPromise = ctx.writeAndFlush(response);

        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void sendOKResponse(String data) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(data, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        sendAndCleanupConnection(ctx, response);
    }

    private void saveFile() throws JsonProcessingException {
        byte[] image = new byte[request.content().readableBytes()];
        request.content().readBytes(image);

        ImageUpload imageUpload = new ImageUpload();
        String filepath = "static/images/" + imageUpload.uuid + ".png";
        fileLookupTable.put(String.valueOf(imageUpload.uuid), filepath);
        try (FileOutputStream fos = new FileOutputStream(filepath)) {
            fos.write(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String serverResponseJSON = objectMapper.writeValueAsString(imageUpload);
        sendOKResponse(serverResponseJSON);
    }

    public void run(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {
        this.ctx = ctx;
        this.request = request;
        if (request.method() == HttpMethod.OPTIONS) {
            sendOKResponse("");
            return;
        }
        saveFile();
    }
}
