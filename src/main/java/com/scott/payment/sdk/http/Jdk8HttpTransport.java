package com.scott.payment.sdk.http;

import com.scott.payment.sdk.exception.OpenApiHttpException;
import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.json.JsonSupport;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : Jdk8HttpTransport
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : 基于 Java 8 HttpURLConnection 的默认 HTTP 传输实现，负责发送 SDK 构造好的 OpenAPI HTTP 请求并读取响应。
 *                本类不生成 JWT、不加密请求、不解密响应，也不判断支付、退款、代付或资金状态；网络异常时由上层查询接口确认业务结果。
 * @status : modify
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
            String body = readBody(statusCode >= OpenApiConstants.HTTP_STATUS_SUCCESS_MIN
                    && statusCode < OpenApiConstants.HTTP_STATUS_SUCCESS_MAX_EXCLUSIVE
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            log.debug("HTTP请求完成: {}", JsonSupport.toJson(logFields(
                    "method", method,
                    "path", request.getUri().getPath(),
                    "statusCode", statusCode,
                    "elapsedMillis", System.currentTimeMillis() - startMillis)));
            return SdkHttpResponse.builder()
                    .statusCode(statusCode)
                    .headers(flattenHeaders(connection.getHeaderFields()))
                    .body(body)
                    .build();
        } catch (IOException exception) {
            throw new OpenApiHttpException("OpenAPI HTTP request failed", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 写入 HTTP 请求体。
     *
     * @param connection HTTP 连接
     * @param body 请求体文本，OpenAPI POST 场景下为加密外壳 JSON
     * @throws IOException 写入失败时抛出
     */
    private void writeBody(HttpURLConnection connection, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(bytes);
        }
    }

    /**
     * 读取 HTTP 响应体。
     *
     * @param inputStream 成功或错误响应流
     * @return 响应体文本
     * @throws IOException 读取失败时抛出
     */
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

    /**
     * 将 HttpURLConnection 多值响应头压平成 SDK Header Map。
     *
     * @param headers 多值响应头
     * @return 单值响应头
     */
    private Map<String, String> flattenHeaders(Map<String, List<String>> headers) {
        Map<String, String> result = new HashMap<String, String>(OpenApiConstants.HTTP_RESPONSE_HEADER_MAP_SIZE);
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

    private static Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
