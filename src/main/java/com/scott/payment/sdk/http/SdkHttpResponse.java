package com.scott.payment.sdk.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

/**
 * SDK HTTP 响应模型，保留 HTTP 状态码、响应头和响应体，供上层统一解析 OpenAPI 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdkHttpResponse {

    /**
     * HTTP 状态码。
     */
    private int statusCode;

    /**
     * HTTP 响应头。
     */
    private Map<String, String> headers;

    /**
     * HTTP 响应体，成功响应中的 data 仍为平台加密 compact 字符串。
     */
    @ToString.Exclude
    private String body;
}
