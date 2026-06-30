package com.wikerx.payment.gateway.sdk;

import com.wikerx.payment.gateway.sdk.config.PaymentGatewayConstants;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayConfigException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.net.URI;
import java.time.Clock;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentGatewayClientConfig
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : SDK 客户端运行配置，负责承载商户号、livemode、JWT API 密钥、平台请求公钥、商户响应私钥和 HTTP 基础参数。
 *                本类只做配置表达和本地完整性校验，不负责发起 HTTP 请求、资金状态处理或外部渠道调用。
 *                API 私钥和商户响应私钥属于敏感数据，必须通过 Lombok toString 排除，避免进入测试日志、普通日志或异常消息。
 * @status : modify
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PaymentGatewayClientConfig {

    /**
     * Payment Gateway 网关基础地址，用于拼接 `/pay-api/**` 请求路径。
     */
    private String baseUrl;

    /**
     * 平台分配的商户号，SDK 会写入 JWT `merchantId` claim。
     */
    private String merchantId;

    /**
     * 商户 JWT HS256 签名密钥，用于生成 `Authorization: Bearer <jwt>`。
     */
    @ToString.Exclude
    private String merchantJwtSecret;

    /**
     * 是否生产模式，false 表示沙盒，true 表示生产。
     */
    private Boolean livemode;

    /**
     * 平台请求公钥，用于加密商户请求体 `data`。
     */
    @ToString.Exclude
    private String platformPublicKey;

    /**
     * 商户响应私钥，用于解密平台成功响应中的 `data`。
     */
    @ToString.Exclude
    private String merchantResponsePrivateKey;

    /**
     * JWT 有效期，单位秒。普通商户配置文件不填写，由 SDK 常量自动设置。
     */
    @Builder.Default
    private Integer jwtTtlSeconds = PaymentGatewayConstants.JWT_TTL_SECONDS;

    /**
     * HTTP 连接超时，单位毫秒。普通商户配置文件不填写，由 SDK 常量自动设置。
     */
    @Builder.Default
    private Integer connectTimeoutMs = PaymentGatewayConstants.HTTP_CONNECT_TIMEOUT_MS;

    /**
     * HTTP 响应读取超时，单位毫秒。普通商户配置文件不填写，由 SDK 常量自动设置。
     */
    @Builder.Default
    private Integer readTimeoutMs = PaymentGatewayConstants.HTTP_READ_TIMEOUT_MS;

    /**
     * 默认接口版本。普通商户配置文件不填写，由 SDK 常量自动设置。
     */
    @Builder.Default
    private String defaultVersion = PaymentGatewayConstants.DEFAULT_VERSION;

    /**
     * SDK 时间源，主要用于测试 JWT 签发时间；商户一般无需设置。
     */
    @Builder.Default
    private Clock clock = Clock.systemUTC();

    /**
     * 校验配置完整性，避免缺少商户号、密钥或基础地址时发出错误请求。
     */
    public void validate() {
        requireText(baseUrl, "payment.gateway.base-url");
        requireText(merchantId, "payment.gateway.merchant-no");
        requireText(merchantJwtSecret, "payment.gateway.api-private-key");
        if (livemode == null) {
            throw new PaymentGatewayConfigException("payment.gateway.livemode can not be null");
        }
        requireText(platformPublicKey, "payment.gateway.platform-request-public-key");
        requireText(merchantResponsePrivateKey, "payment.gateway.merchant-response-private-key");
        if (jwtTtlSeconds == null || jwtTtlSeconds <= 0 || jwtTtlSeconds > PaymentGatewayConstants.JWT_TTL_SECONDS) {
            throw new PaymentGatewayConfigException("jwtTtlSeconds must be between 1 and " + PaymentGatewayConstants.JWT_TTL_SECONDS);
        }
        if (connectTimeoutMs == null || connectTimeoutMs <= 0) {
            throw new PaymentGatewayConfigException("connectTimeoutMs must be positive");
        }
        if (readTimeoutMs == null || readTimeoutMs <= 0) {
            throw new PaymentGatewayConfigException("readTimeoutMs must be positive");
        }
        if (clock == null) {
            throw new PaymentGatewayConfigException("clock can not be null");
        }
        defaultVersion = requireText(defaultVersion, "defaultVersion");
        baseUrl = normalizeBaseUrl(baseUrl);
        merchantId = merchantId.trim();
        merchantJwtSecret = merchantJwtSecret.trim();
        platformPublicKey = platformPublicKey.trim();
        merchantResponsePrivateKey = merchantResponsePrivateKey.trim();
    }

    /**
     * 返回标准化后的 OpenAPI 基础 URI。
     *
     * @return OpenAPI 基础 URI
     */
    public URI getBaseUri() {
        return URI.create(normalizeBaseUrl(baseUrl));
    }

    /**
     * 兼容旧版高级用法，将直接字段封装为安全材料对象。
     *
     * @return 商户安全材料
     */
    public PaymentGatewayCredentials getCredentials() {
        return new PaymentGatewayCredentials(merchantId, merchantJwtSecret, platformPublicKey, merchantResponsePrivateKey);
    }

    private static String normalizeBaseUrl(String value) {
        String text = requireText(value, "payment.gateway.base-url");
        return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new PaymentGatewayConfigException(fieldName + " can not be blank");
        }
        return value.trim();
    }
}
