package com.wikerx.payment.gateway.sdk.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.net.URI;
import java.util.Map;

/**
 * SDK HTTP 请求模型，隔离底层 HTTP 客户端细节，便于商户替换传输层或在测试中模拟响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdkHttpRequest {

    /**
     * HTTP 方法。为空时默认使用 POST，兼容旧测试和旧扩展传输层。
     */
    private String method;

    /**
     * 请求完整地址。
     */
    private URI uri;

    /**
     * HTTP 请求头。不得在日志中输出 authorization 或密文 data。
     */
    @ToString.Exclude
    private Map<String, String> headers;

    /**
     * HTTP 请求体，OpenAPI 场景下为只包含 data 字段的密文 JSON。
     */
    @ToString.Exclude
    private String body;

    /**
     * HTTP 连接建立超时时间，单位毫秒。
     */
    private int connectTimeoutMs;

    /**
     * 响应读取超时时间，单位毫秒。
     */
    private int readTimeoutMs;
}
