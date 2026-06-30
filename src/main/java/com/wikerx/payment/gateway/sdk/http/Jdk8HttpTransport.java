package com.wikerx.payment.gateway.sdk.http;

import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayHttpException;
import com.wikerx.payment.gateway.sdk.config.PaymentGatewayConstants;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Java 8 HttpURLConnection 的默认 HTTP 传输实现。
 */
@Slf4j
public class Jdk8HttpTransport implements HttpTransport {

    /**
     * 执行 HTTP 请求。
     *
     * @param request SDK HTTP 请求
     * @return SDK HTTP 响应
     */
    @Override
    public SdkHttpResponse execute(SdkHttpRequest request) {
        long startMillis = System.currentTimeMillis();
        HttpURLConnection connection = null;
        try {
            String method = request.getMethod() == null || request.getMethod().trim().isEmpty()
                    ? "POST" : request.getMethod().trim().toUpperCase();
            connection = (HttpURLConnection) request.getUri().toURL().openConnection();
            connection.setRequestMethod(method);
            boolean hasBody = request.getBody() != null && !request.getBody().isEmpty();
            connection.setDoOutput(hasBody);
            connection.setConnectTimeout(request.getConnectTimeoutMs());
            connection.setReadTimeout(request.getReadTimeoutMs());
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            if (hasBody) {
                writeBody(connection, request.getBody());
            }
            int statusCode = connection.getResponseCode();
            String body = readBody(statusCode >= PaymentGatewayConstants.HTTP_STATUS_SUCCESS_MIN
                    && statusCode < PaymentGatewayConstants.HTTP_STATUS_SUCCESS_MAX_EXCLUSIVE
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            log.debug("event=payment_gateway_sdk_http_completed method={} path={} statusCode={} elapsedMillis={}",
                    method,
                    request.getUri().getPath(),
                    statusCode,
                    System.currentTimeMillis() - startMillis);
            return SdkHttpResponse.builder()
                    .statusCode(statusCode)
                    .headers(flattenHeaders(connection.getHeaderFields()))
                    .body(body)
                    .build();
        } catch (IOException exception) {
            throw new PaymentGatewayHttpException("OpenAPI HTTP request failed", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void writeBody(HttpURLConnection connection, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(bytes);
        }
    }

    private String readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        byte[] buffer = new byte[4096];
        int length;
        try (InputStream source = inputStream;
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            while ((length = source.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, length);
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> flattenHeaders(Map<String, List<String>> headers) {
        Map<String, String> result = new HashMap<String, String>(PaymentGatewayConstants.HTTP_RESPONSE_HEADER_MAP_SIZE);
        if (headers == null) {
            return result;
        }
        // HttpURLConnection 的响应头可能有多个值，SDK 当前只需要保留首个值用于调试和扩展。
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                result.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return result;
    }
}
