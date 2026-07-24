package com.scott.payment.sdk.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

/**
 * HTTP request header and body log sanitizer.
 */
public final class RequestHeaderParams {

    private static final String MASKED_VALUE = "******";

    private static final Set<String> SENSITIVE_HEADERS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "authorization",
                    "proxy-authorization",
                    "cookie",
                    "set-cookie",
                    "x-api-key",
                    "api-key",
                    "signature",
                    "x-signature",
                    "x-webhook-signature"
            )));

    private static final Set<String> SENSITIVE_BODY_FIELDS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "password",
                    "pwd",
                    "token",
                    "accesstoken",
                    "access_token",
                    "refreshtoken",
                    "refresh_token",
                    "cardnumber",
                    "card_number",
                    "cardno",
                    "pan",
                    "cvv",
                    "cvv2",
                    "cvc",
                    "cvc2",
                    "securitycode",
                    "security_code"
            )));

    private RequestHeaderParams() {
        throw new IllegalStateException("Utility class cannot be instantiated");
    }

    /**
     * 获取并脱敏全部 HTTP 请求头。
     *
     * @param request HTTP 请求对象
     * @return 请求头集合，Key 为请求头名称，Value 为请求头值列表
     */
    public static Map<String, List<String>> getRequestHeaders(HttpServletRequest request) {
        Objects.requireNonNull(request, "HttpServletRequest must not be null");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            List<String> headerValues = Collections.list(request.getHeaders(headerName));
            headers.put(headerName, sanitizeHeaderValues(headerName, headerValues));
        }
        return headers;
    }

    /**
     * 获取适合写入日志的请求 Body。
     *
     * @param requestBody 原始请求 Body
     * @param objectMapper Jackson ObjectMapper
     * @return 适合写入日志的请求 Body
     */
    public static String getRequestBodyForLog(String requestBody, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(requestBody)) {
            return "";
        }
        Objects.requireNonNull(objectMapper, "ObjectMapper must not be null");
        try {
            JsonNode rootNode = objectMapper.readTree(requestBody);
            maskSensitiveJsonFields(rootNode);

            return sanitizeLogValue(objectMapper.writeValueAsString(rootNode));
        } catch (Exception exception) {
            // Controller receives body as String, so invalid JSON still needs safe log output.
            return sanitizeLogValue(requestBody);
        }
    }

    private static List<String> sanitizeHeaderValues(String headerName, List<String> headerValues) {
        if (isSensitiveHeader(headerName)) {
            return Collections.singletonList(MASKED_VALUE);
        }

        return headerValues.stream()
                .map(RequestHeaderParams::sanitizeLogValue)
                .collect(Collectors.toList());
    }

    private static void maskSensitiveJsonFields(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return;
        }

        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;

            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);

            for (String fieldName : fieldNames) {
                JsonNode fieldValue = objectNode.get(fieldName);

                if (isSensitiveBodyField(fieldName)) {
                    objectNode.put(fieldName, MASKED_VALUE);
                    continue;
                }

                maskSensitiveJsonFields(fieldValue);
            }

            return;
        }

        if (jsonNode.isArray()) {
            jsonNode.forEach(RequestHeaderParams::maskSensitiveJsonFields);
        }
    }

    private static boolean isSensitiveHeader(String headerName) {
        return StringUtils.hasText(headerName)
                && SENSITIVE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private static boolean isSensitiveBodyField(String fieldName) {
        return StringUtils.hasText(fieldName)
                && SENSITIVE_BODY_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT));
    }

    private static String sanitizeLogValue(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}
