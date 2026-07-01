package org.realityforge.bazel.depgen.metadata;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jspecify.annotations.NonNull;

final class TinyHttpd {
    @NonNull
    private final HttpServer _server;

    @NonNull
    private final ExecutorService _executor = Executors.newCachedThreadPool();

    @NonNull
    private HttpHandler _handler = exchange -> {
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    };

    TinyHttpd() throws IOException {
        _server = HttpServer.create(new InetSocketAddress(InetAddress.getLocalHost(), 0), 0);
        _server.createContext("/", exchange -> _handler.handle(exchange));
        _server.setExecutor(_executor);
    }

    void setHttpHandler(@NonNull final HttpHandler handler) {
        _handler = handler;
    }

    void start() {
        _server.start();
    }

    void stop() {
        _server.stop(0);
        _executor.shutdownNow();
    }

    @NonNull
    String getBaseURL() {
        final InetSocketAddress address = _server.getAddress();
        return "http://" + address.getAddress().getCanonicalHostName() + ":" + address.getPort() + "/";
    }
}
