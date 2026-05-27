package org.example.router;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.SystemPropertyUtil;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class EndpointRouter {
    private final ConcurrentHashMap<String, IHandler> routerLookupHashmap = new ConcurrentHashMap<>();

    public void add(String endpoint, IHandler handler) {
        routerLookupHashmap.put(endpoint, handler);
    }

    private String sanitizeUri(String uri) {
        final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
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

    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) throws IOException {
        String uri = request.uri();
        final String path = sanitizeUri(uri);

        assert path != null;
        String[] splitedString = path.split("//");
        String endpoint = splitedString[splitedString.length - 1];

        IHandler handler;
        if (routerLookupHashmap.containsKey(endpoint)) {
            handler = routerLookupHashmap.get(endpoint);
        } else {
            handler = routerLookupHashmap.get("default");
        }

        handler.run(ctx, request);
    }
}
