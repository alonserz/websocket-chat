package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;
import org.example.messages.ImageUpload;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.example.ChatServerInitializer.fileLookupTable;

class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String websocketURI;
    private FullHttpRequest request;

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private HttpPostRequestDecoder decoder;

    public HttpRequestHandler(String websocketURI) {
        this.websocketURI = websocketURI;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (websocketURI.equalsIgnoreCase(request.uri())) {
            ctx.fireChannelRead(request.retain());
        } else {
            final boolean keepAlive = HttpUtil.isKeepAlive(request);
            this.request = request;
            String uri = request.uri();
            final String path = sanitizeUri(uri);
            ImageUpload imageUpload = new ImageUpload();
            assert path != null;
            if (path.endsWith("upload")) {
                if (request.method() == HttpMethod.OPTIONS) {
                    // вонючий preflight
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer("", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                    response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
                    sendAndCleanupConnection(ctx, response);
                    return;
                }
                byte[] image = new byte[request.content().readableBytes()];
                request.content().readBytes(image);

                String filepath = "static/images/" + imageUpload.uuid + ".png";
                fileLookupTable.put(String.valueOf(imageUpload.uuid), filepath);
                try (FileOutputStream fos = new FileOutputStream(filepath)) {
                    fos.write(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                String serverResponseJSON = objectMapper.writeValueAsString(imageUpload);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HTTP_1_1, OK, Unpooled.copiedBuffer(serverResponseJSON, CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "*");
                sendAndCleanupConnection(ctx, response);
            } else {
                File file = new File(path);
                if (file.isHidden() || !file.exists()) {
                    return;
                }

                if (!file.isFile()) {
                    sendError(ctx, FORBIDDEN);
                    return;
                }

                RandomAccessFile raf;
                try {
                    raf = new RandomAccessFile(file, "r");
                } catch (FileNotFoundException ignore) {
                    sendError(ctx, NOT_FOUND);
                    return;
                }
                long fileLength = raf.length();

                HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
                HttpUtil.setContentLength(response, fileLength);
                setContentTypeHeader(response);
                setDateAndCacheHeaders(response, file);

                if (!keepAlive) {
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                } else if (request.protocolVersion().equals(HTTP_1_0)) {
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                }

                // Write the initial line and the header.
                ctx.write(response);

                // Write the content.
                ChannelFuture sendFileFuture;
                ChannelFuture lastContentFuture;
                if (ctx.pipeline().get(SslHandler.class) == null) {
                    ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
                    // Write the end marker.
                    lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                } else {
                    sendFileFuture =
                            ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
                                    ctx.newProgressivePromise());
                    // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                    lastContentFuture = sendFileFuture;
                }

                // Decide whether to close the connection or not.
                if (!keepAlive) {
                    // Close the connection when the whole content is written out.
                    lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
    }

    private static String sanitizeUri(String uri) {
        // Decode the path.
        uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);

        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        // Convert file separators.
        uri = uri.replace('/', File.separatorChar);

        // Simplistic dumb security check.
        // You will have to do something serious in the production environment.
        if (uri.contains(File.separator + '.') ||
                uri.contains('.' + File.separator) ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.' ||
                INSECURE_URI.matcher(uri).matches()) {
            return null;
        }

        // Convert to absolute path.
        return SystemPropertyUtil.get("user.dir") + File.separator + uri;
    }

    private void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpResponse response) {
        final FullHttpRequest request = this.request;
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

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        sendAndCleanupConnection(ctx, response);
    }

    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    private static void setContentTypeHeader(HttpResponse response) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/png");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}