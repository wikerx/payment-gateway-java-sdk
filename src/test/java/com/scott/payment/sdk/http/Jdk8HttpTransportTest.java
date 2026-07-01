package com.scott.payment.sdk.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class Jdk8HttpTransportTest {

    /**
     * 验证 JDK HTTP 传输层可发送无请求体 GET。
     */
    @Test
    void shouldExecuteGetWithoutBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ping", new JsonHandler());
        server.start();
        try {
            URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/ping");
            SdkHttpResponse response = new Jdk8HttpTransport().execute(SdkHttpRequest.builder()
                    .method("GET")
                    .uri(uri)
                    .headers(Collections.singletonMap("Accept", "application/json"))
                    .connectTimeoutMs(1000)
                    .readTimeoutMs(1000)
                    .build());

            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getBody()).contains("\"ok\":true");
        } finally {
            server.stop(0);
        }
    }

    /**
     * 测试用 JSON 响应处理器。
     */
    static class JsonHandler implements HttpHandler {
        /**
         * 处理测试 HTTP 请求。
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] bytes = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }
}
