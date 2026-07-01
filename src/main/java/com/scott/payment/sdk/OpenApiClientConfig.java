package com.scott.payment.sdk;

import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.exception.OpenApiConfigException;
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
 * @classname : OpenApiClientConfig
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : SDK 客户端运行配置，负责承载商户号、livemode、JWT API 密钥、平台请求公钥、商户响应私钥和 HTTP 基础参数。
 *                本类只做配置表达和本地完整性校验，不负责发起 HTTP 请求、资金状态处理或外部渠道调用。
 *                API 私钥和商户响应私钥属于敏感数据，必须通过 Lombok toString 排除；原始 HTTP 日志只用于沙盒联调核验。
 * @status : modify
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OpenApiClientConfig {

    /**
     * OpenAPI 网关基础地址，用于拼接 `/pay-api/**` 请求路径。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 格式：HTTP 或 HTTPS URL，不以 `/` 结尾也可由 SDK 标准化。
     */
    private String baseUrl;

    /**
     * 平台分配的商户号，SDK 会写入 JWT `merchantId` claim。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与网关商户身份识别、数据隔离和日志排查。
     */
    private String merchantId;

    /**
     * 商户 JWT HS256 签名密钥，用于生成 `Authorization: Bearer <jwt>`。
     *
     * 敏感字段：是。
     * 是否允许为空：否。
     * 是否参与签名：是。
     * 限制：不得进入 toString、普通日志或异常消息。
     */
    @ToString.Exclude
    private String merchantJwtSecret;

    /**
     * 是否生产模式，false 表示沙盒，true 表示生产。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：写入 JWT 和 POST 请求外壳，参与网关数据源路由和响应校验。
     */
    private Boolean livemode;

    /**
     * 平台请求公钥，用于加密商户请求体 `data`。
     *
     * 敏感字段：按密钥材料处理。
     * 是否允许为空：否。
     * 格式：PEM 或 X.509 DER Base64。
     * 是否参与加密：是。
     */
    @ToString.Exclude
    private String platformPublicKey;

    /**
     * 商户响应私钥，用于解密平台成功响应中的 `data`。
     *
     * 敏感字段：是。
     * 是否允许为空：否。
     * 格式：PEM 或 PKCS#8 DER Base64。
     * 是否参与解密：是。
     * 限制：不得进入 toString、普通日志、异常消息或客户端环境。
     */
    @ToString.Exclude
    private String merchantResponsePrivateKey;

    /**
     * JWT 有效期，单位秒。普通商户配置文件不填写，由 SDK 常量自动设置。
     */
    @Builder.Default
    private Integer jwtTtlSeconds = OpenApiConstants.JWT_TTL_SECONDS;

    /**
     * HTTP 连接超时，单位毫秒。普通商户配置文件不填写，由 SDK 常量自动设置。
     */
    @Builder.Default
    private Integer connectTimeoutMs = OpenApiConstants.HTTP_CONNECT_TIMEOUT_MS;

    /**
     * HTTP 响应读取超时，单位毫秒。普通商户配置文件不填写，由 SDK 常量自动设置。
     */
    @Builder.Default
    private Integer readTimeoutMs = OpenApiConstants.HTTP_READ_TIMEOUT_MS;

    /**
     * 默认接口版本。普通商户配置文件不填写，由 SDK 常量自动设置。
     */
    @Builder.Default
    private String defaultVersion = OpenApiConstants.DEFAULT_VERSION;

    /**
     * 是否打印原始 HTTP 调试日志。
     *
     * 敏感字段：是。
     * 是否允许为空：允许为空，空值按 false 处理。
     * 用途：沙盒联调时输出完整请求地址、请求头、请求报文和响应报文，便于商户核验加密后的实际传输数据。
     * 限制：生产环境不建议开启；开启后会输出明文业务对象和完整密文 data，Authorization JWT 仍会脱敏。
     */
    @Builder.Default
    private Boolean rawHttpLogEnabled = Boolean.FALSE;

    /**
     * SDK 时间源，主要用于测试 JWT 签发时间；商户一般无需设置。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与 JWT iat、exp 计算。
     */
    @Builder.Default
    private Clock clock = Clock.systemUTC();

    /**
     * 校验配置完整性，避免缺少商户号、livemode、密钥或基础地址时发出错误请求。
     *
     * 该方法只做本地字段校验和格式标准化，不发起 HTTP 请求、不修改资金状态、不轮换密钥。
     *
     * @throws OpenApiConfigException 配置缺失、超时非法或时间源为空时抛出
     */
    public void validate() {
        requireText(baseUrl, "payment.gateway.base-url");
        requireText(merchantId, "payment.gateway.merchant-no");
        requireText(merchantJwtSecret, "payment.gateway.api-private-key");
        if (livemode == null) {
            throw new OpenApiConfigException("payment.gateway.livemode can not be null");
        }
        requireText(platformPublicKey, "payment.gateway.platform-request-public-key");
        requireText(merchantResponsePrivateKey, "payment.gateway.merchant-response-private-key");
        if (jwtTtlSeconds == null || jwtTtlSeconds <= 0 || jwtTtlSeconds > OpenApiConstants.JWT_TTL_SECONDS) {
            throw new OpenApiConfigException("jwtTtlSeconds must be between 1 and " + OpenApiConstants.JWT_TTL_SECONDS);
        }
        if (connectTimeoutMs == null || connectTimeoutMs <= 0) {
            throw new OpenApiConfigException("connectTimeoutMs must be positive");
        }
        if (readTimeoutMs == null || readTimeoutMs <= 0) {
            throw new OpenApiConfigException("readTimeoutMs must be positive");
        }
        if (clock == null) {
            throw new OpenApiConfigException("clock can not be null");
        }
        if (rawHttpLogEnabled == null) {
            rawHttpLogEnabled = Boolean.FALSE;
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
    public OpenApiCredentials getCredentials() {
        return new OpenApiCredentials(merchantId, merchantJwtSecret, platformPublicKey, merchantResponsePrivateKey);
    }

    private static String normalizeBaseUrl(String value) {
        String text = requireText(value, "payment.gateway.base-url");
        return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new OpenApiConfigException(fieldName + " can not be blank");
        }
        return value.trim();
    }
}
