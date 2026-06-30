package com.wikerx.payment.gateway.sdk.config;

import com.wikerx.payment.gateway.sdk.json.JsonSupport;
import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayConfigException;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/**
 * SDK properties 配置模型，支持商户从 classpath 或外部文件加载基础地址、商户号、密钥和超时配置。
 */
@Data
@ToString
public class PaymentGatewayProperties {

    /**
     * 当前支持的 JWT 算法。
     */
    private static final String SUPPORTED_JWT_ALGORITHM = "HS256";
    /**
     * 当前支持的 payload 加密算法。
     */
    private static final String SUPPORTED_CRYPTO_ALGORITHM = "RSA-OAEP-256 + AES-256-GCM";

    /**
     * OpenAPI 网关基础地址，例如 https://openapi.example.com。
     * <p>
     * SDK 在每次发起 HTTP 请求时使用该地址拼接 `/pay-api/**` 路径。
     */
    private String baseUri;

    /**
     * 默认接口版本，例如 v1。
     * <p>
     * 调用 `authorizePayment(request)` 这类未显式传 version 的方法时使用。
     */
    private String defaultVersion = PaymentGatewayConstants.DEFAULT_VERSION;

    /**
     * 平台分配的商户号。
     * <p>
     * SDK 在签发 JWT 时写入 `merchantId` claim，后端会用它定位商户密钥和 RSA 私钥。
     */
    private String merchantId;

    /**
     * 商户名称，仅用于商户侧识别配置归属。
     * <p>
     * SDK 不会把该字段写入请求，也不会参与签名、加密或解密。
     */
    private String merchantName;

    /**
     * JWT 签名算法。
     * <p>
     * 当前后端只支持 HS256，SDK 在加载配置时会校验该字段，避免误用其他算法。
     */
    private String jwtAlgorithm = SUPPORTED_JWT_ALGORITHM;

    /**
     * JWT 有效期，单位秒。
     * <p>
     * SDK 在每次请求前签发 JWT 时使用，后端当前最大允许 180 秒。
     */
    private long jwtExpiresSeconds = PaymentGatewayConstants.JWT_TTL_SECONDS;

    /**
     * 商户 JWT HS256 签名密钥。
     * <p>
     * SDK 在每次请求前使用该密钥签发 `Authorization: Bearer <jwt>`，后端使用同一密钥验签。
     */
    @ToString.Exclude
    private String merchantKey;

    /**
     * 商户 JWT 签名密钥指纹。
     * <p>
     * 该字段用于人工核对或配置审计，SDK 不用它参与签名。
     */
    private String merchantKeyFingerprint;

    /**
     * 请求加密算法说明。
     * <p>
     * 当前后端固定为 RSA-OAEP-256 + AES-256-GCM，SDK 加载配置时会校验该字段。
     */
    private String requestCryptoAlgorithm = SUPPORTED_CRYPTO_ALGORITHM;

    /**
     * 平台请求公钥，X.509 DER Base64 或 PEM。
     * <p>
     * SDK 在每次请求前使用该公钥加密请求体 `data`；只有平台持有对应私钥才能解密。
     */
    private String platformPublicKeyX509Base64;

    /**
     * 平台请求公钥 PEM 文件路径。
     * <p>
     * 支持 `classpath:`、`file:` 或普通文件路径；配置后优先于 `platformPublicKeyX509Base64`。
     */
    private String platformPublicKeyFile;

    /**
     * 平台请求公钥指纹。
     * <p>
     * 该字段用于商户侧人工核对平台公钥是否匹配，SDK 不用它参与加密。
     */
    private String platformPublicKeyFingerprint;

    /**
     * 响应加密算法说明。
     * <p>
     * 当前后端固定为 RSA-OAEP-256 + AES-256-GCM，SDK 加载配置时会校验该字段。
     */
    private String responseCryptoAlgorithm = SUPPORTED_CRYPTO_ALGORITHM;

    /**
     * 商户响应公钥，X.509 DER Base64 或 PEM。
     * <p>
     * 平台在生成成功响应时使用该公钥加密响应 `data`；商户 SDK 运行时不需要使用公钥。
     */
    private String merchantResponsePublicKeyX509Base64;

    /**
     * 商户响应私钥，PKCS#8 DER Base64 或 PEM。
     * <p>
     * SDK 在收到成功响应后使用该私钥解密响应 `data`，商户必须妥善保管，不得写入日志。
     */
    @ToString.Exclude
    private String merchantResponsePrivateKeyPkcs8Base64;

    /**
     * 商户响应私钥 PEM 文件路径。
     * <p>
     * 支持 `classpath:`、`file:` 或普通文件路径；配置后优先于 `merchantResponsePrivateKeyPkcs8Base64`。
     */
    @ToString.Exclude
    private String merchantResponsePrivateKeyFile;

    /**
     * 商户响应公钥指纹。
     * <p>
     * 该字段用于人工核对平台侧登记的商户响应公钥，SDK 不用它参与解密。
     */
    private String merchantResponsePublicKeyFingerprint;

    /**
     * 兼容旧版 SDK 配置名：商户 JWT HS256 签名密钥。
     */
    @Deprecated
    @ToString.Exclude
    private String merchantJwtSecret;

    /**
     * 兼容旧版 SDK 配置名：平台请求公钥。
     */
    @Deprecated
    @ToString.Exclude
    private String platformPublicKey;

    /**
     * 兼容旧版 SDK 配置名：商户响应私钥。
     */
    @Deprecated
    @ToString.Exclude
    private String merchantResponsePrivateKey;

    /**
     * HTTP 连接超时时间，单位毫秒。
     * <p>
     * SDK 创建 JDK HttpClient 时使用。
     */
    private long connectTimeoutMillis = PaymentGatewayConstants.HTTP_CONNECT_TIMEOUT_MS;

    /**
     * HTTP 请求超时时间，单位毫秒。
     * <p>
     * SDK 每次发送 POST 请求时设置到 HTTP request timeout。
     */
    private long requestTimeoutMillis = PaymentGatewayConstants.HTTP_READ_TIMEOUT_MS;

    /**
     * 从 classpath 加载默认商户配置文件。
     *
     * @return SDK properties 配置
     */
    public static PaymentGatewayProperties loadDefault() {
        return loadFromClasspath(PaymentGatewayConstants.CONFIG_FILE_NAME);
    }

    /**
     * 从 classpath 加载 SDK 配置文件。
     *
     * @param classpathLocation classpath 下的配置文件路径
     * @return SDK properties 配置
     */
    public static PaymentGatewayProperties loadFromClasspath(String classpathLocation) {
        String location = requireText(classpathLocation, "classpathLocation");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(location)) {
            if (inputStream == null) {
                throw new PaymentGatewayConfigException("OpenAPI SDK properties file not found in classpath: " + location);
            }
            return load(inputStream);
        } catch (IOException exception) {
            throw new PaymentGatewayConfigException("OpenAPI SDK properties file can not be read: " + location, exception);
        }
    }

    /**
     * 从输入流加载 SDK 配置文件。
     *
     * @param inputStream properties 输入流
     * @return SDK properties 配置
     */
    public static PaymentGatewayProperties load(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream can not be null");
        Properties source = new Properties();
        try {
            source.load(inputStream);
        } catch (IOException exception) {
            throw new PaymentGatewayConfigException("OpenAPI SDK properties can not be loaded", exception);
        }
        PaymentGatewayProperties properties = new PaymentGatewayProperties();
        properties.setBaseUri(firstProperty(source, "merchant.openapi.base-url", "scott.openapi.base-url", "baseUri"));
        properties.setDefaultVersion(firstPropertyOrDefault(source, PaymentGatewayConstants.DEFAULT_VERSION, "scott.openapi.default-version", "defaultVersion"));
        properties.setMerchantId(firstProperty(source, "merchant.id", "scott.openapi.merchant-id", "merchantId"));
        properties.setMerchantName(firstProperty(source, "scott.openapi.merchant-name", "merchantName"));
        properties.setJwtAlgorithm(firstPropertyOrDefault(source, SUPPORTED_JWT_ALGORITHM, "scott.openapi.jwt.algorithm", "jwtAlgorithm"));
        properties.setJwtExpiresSeconds(PaymentGatewayConstants.JWT_TTL_SECONDS);
        properties.setMerchantKey(firstProperty(source, "merchant.api.private-key", "merchant.jwt.secret", "scott.openapi.jwt.secret", "merchantKey"));
        properties.setMerchantKeyFingerprint(firstProperty(source, "scott.openapi.jwt.secret-fingerprint", "merchantKeyFingerprint"));
        properties.setRequestCryptoAlgorithm(firstPropertyOrDefault(source, SUPPORTED_CRYPTO_ALGORITHM, "scott.openapi.crypto.request-algorithm", "requestCryptoAlgorithm"));
        properties.setPlatformPublicKeyFile(firstProperty(source, "merchant.platform.public-key-file", "scott.openapi.crypto.platform-public-key-file", "platformPublicKeyFile"));
        properties.setPlatformPublicKeyX509Base64(firstProperty(source, "merchant.platform.public-key", "scott.openapi.crypto.platform-public-key", "platformPublicKeyX509Base64"));
        properties.setPlatformPublicKeyFingerprint(firstProperty(source, "scott.openapi.crypto.platform-public-key-fingerprint", "platformPublicKeyFingerprint"));
        properties.setResponseCryptoAlgorithm(firstPropertyOrDefault(source, SUPPORTED_CRYPTO_ALGORITHM, "scott.openapi.crypto.response-algorithm", "responseCryptoAlgorithm"));
        properties.setMerchantResponsePublicKeyX509Base64(firstProperty(source, "scott.openapi.crypto.merchant-response-public-key", "merchantResponsePublicKeyX509Base64"));
        properties.setMerchantResponsePrivateKeyFile(firstProperty(source, "merchant.response.private-key-file", "scott.openapi.crypto.merchant-response-private-key-file", "merchantResponsePrivateKeyFile"));
        properties.setMerchantResponsePrivateKeyPkcs8Base64(firstProperty(source, "merchant.response.private-key", "scott.openapi.crypto.merchant-response-private-key", "merchantResponsePrivateKeyPkcs8Base64"));
        properties.setMerchantResponsePublicKeyFingerprint(firstProperty(source, "scott.openapi.crypto.merchant-response-public-key-fingerprint", "merchantResponsePublicKeyFingerprint"));
        properties.setConnectTimeoutMillis(PaymentGatewayConstants.HTTP_CONNECT_TIMEOUT_MS);
        properties.setRequestTimeoutMillis(PaymentGatewayConstants.HTTP_READ_TIMEOUT_MS);
        properties.validate();
        return properties;
    }

    /**
     * 从 JSON 输入流加载 SDK 配置。
     * <p>
     * JSON 字段可直接使用平台导出的商户安全资料字段名，例如 merchantId、merchantKey、
     * platformPublicKeyX509Base64、merchantResponsePrivateKeyPkcs8Base64。JSON 中必须包含 baseUri，
     * 或调用 {@link #loadJson(InputStream, String)} 由调用方显式传入。
     *
     * @param inputStream JSON 输入流
     * @return SDK properties 配置
     */
    public static PaymentGatewayProperties loadJson(InputStream inputStream) {
        return loadJson(inputStream, null);
    }

    /**
     * 从 JSON 输入流加载 SDK 配置，并补充 OpenAPI 网关基础地址。
     *
     * @param inputStream JSON 输入流
     * @param baseUri     OpenAPI 网关基础地址
     * @return SDK properties 配置
     */
    public static PaymentGatewayProperties loadJson(InputStream inputStream, String baseUri) {
        Objects.requireNonNull(inputStream, "inputStream can not be null");
        try {
            String json = new String(readAll(inputStream), StandardCharsets.UTF_8);
            PaymentGatewayProperties properties = JsonSupport.fromJson(json, PaymentGatewayProperties.class);
            if (StringUtils.isNotBlank(baseUri)) {
                properties.setBaseUri(baseUri);
            }
            properties.validate();
            return properties;
        } catch (IOException exception) {
            throw new PaymentGatewayConfigException("OpenAPI SDK json properties can not be read", exception);
        }
    }

    /**
     * 转换为 SDK 客户端配置。
     *
     * @return SDK 客户端配置
     */
    public PaymentGatewayClientConfig toClientConfig() {
        validate();
        return PaymentGatewayClientConfig.builder()
                .baseUrl(baseUri)
                .merchantId(merchantId)
                .merchantJwtSecret(runtimeMerchantKey())
                .platformPublicKey(runtimePlatformPublicKey())
                .merchantResponsePrivateKey(runtimeMerchantResponsePrivateKey())
                .defaultVersion(defaultVersion)
                .connectTimeoutMs(toInt(connectTimeoutMillis, "connectTimeoutMillis"))
                .readTimeoutMs(toInt(requestTimeoutMillis, "requestTimeoutMillis"))
                .jwtTtlSeconds(toInt(jwtExpiresSeconds, "jwtExpiresSeconds"))
                .build();
    }

    /**
     * 校验 SDK 配置完整性，避免缺少密钥或商户号时发出错误请求。
     */
    public void validate() {
        requireText(baseUri, "scott.openapi.base-url");
        requireText(defaultVersion, "scott.openapi.default-version");
        requireText(merchantId, "scott.openapi.merchant-id");
        requireText(runtimeMerchantKey(), "scott.openapi.jwt.secret");
        requireText(runtimePlatformPublicKey(), "scott.openapi.crypto.platform-public-key or scott.openapi.crypto.platform-public-key-file");
        requireText(runtimeMerchantResponsePrivateKey(), "scott.openapi.crypto.merchant-response-private-key or scott.openapi.crypto.merchant-response-private-key-file");
        validateAlgorithm(jwtAlgorithm, SUPPORTED_JWT_ALGORITHM, "scott.openapi.jwt.algorithm");
        validateAlgorithm(requestCryptoAlgorithm, SUPPORTED_CRYPTO_ALGORITHM, "scott.openapi.crypto.request-algorithm");
        validateAlgorithm(responseCryptoAlgorithm, SUPPORTED_CRYPTO_ALGORITHM, "scott.openapi.crypto.response-algorithm");
        if (connectTimeoutMillis <= 0L) {
            throw new PaymentGatewayConfigException("connectTimeoutMillis must be positive");
        }
        if (requestTimeoutMillis <= 0L) {
            throw new PaymentGatewayConfigException("requestTimeoutMillis must be positive");
        }
        if (jwtExpiresSeconds <= 0L || jwtExpiresSeconds > PaymentGatewayConstants.JWT_TTL_SECONDS) {
            throw new PaymentGatewayConfigException("jwtExpiresSeconds must be between 1 and " + PaymentGatewayConstants.JWT_TTL_SECONDS);
        }
    }

    private String runtimeMerchantKey() {
        // 优先读取平台导出的新字段 merchantKey，保留旧字段仅为兼容早期 SDK 配置。
        return StringUtils.firstNonBlank(merchantKey, merchantJwtSecret);
    }

    private String runtimePlatformPublicKey() {
        String inlineValue = StringUtils.firstNonBlank(platformPublicKeyX509Base64, platformPublicKey);
        return KeyFileLoader.resolve(platformPublicKeyFile, inlineValue, "merchant.platform.public-key");
    }

    private String runtimeMerchantResponsePrivateKey() {
        String inlineValue = StringUtils.firstNonBlank(merchantResponsePrivateKeyPkcs8Base64, merchantResponsePrivateKey);
        return KeyFileLoader.resolve(merchantResponsePrivateKeyFile, inlineValue, "merchant.response.private-key");
    }

    private static void validateAlgorithm(String actual, String expected, String fieldName) {
        if (StringUtils.isNotBlank(actual) && !expected.equalsIgnoreCase(actual.trim())) {
            throw new PaymentGatewayConfigException(fieldName + " must be " + expected);
        }
    }

    private static String firstProperty(Properties source, String... keys) {
        return firstPropertyOrDefault(source, null, keys);
    }

    private static String firstPropertyOrDefault(Properties source, String defaultValue, String... keys) {
        for (String key : keys) {
            String value = source.getProperty(key);
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return defaultValue;
    }

    private static String requireText(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new PaymentGatewayConfigException(fieldName + " can not be blank");
        }
        return value.trim();
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    private static int toInt(long value, String fieldName) {
        if (value > Integer.MAX_VALUE) {
            throw new PaymentGatewayConfigException(fieldName + " must be less than " + Integer.MAX_VALUE);
        }
        return (int) value;
    }
}
