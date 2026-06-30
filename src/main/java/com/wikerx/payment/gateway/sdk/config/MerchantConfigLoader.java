package com.wikerx.payment.gateway.sdk.config;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 商户配置加载器，负责从 classpath 读取 `merchant-config.properties` 并封装为 SDK 运行配置。
 */
public final class MerchantConfigLoader {

    private MerchantConfigLoader() {
    }

    /**
     * 加载默认商户配置文件。
     *
     * @return SDK 客户端配置
     */
    public static PaymentGatewayClientConfig load() {
        return load(PaymentGatewayConstants.CONFIG_FILE_NAME);
    }

    /**
     * 加载指定 classpath 配置文件，适用于测试或同一应用内多商户场景。
     *
     * @param configFileName classpath 下的配置文件名
     * @return SDK 客户端配置
     */
    public static PaymentGatewayClientConfig load(String configFileName) {
        Properties properties = loadProperties(configFileName);
        return PaymentGatewayClientConfig.builder()
                .baseUrl(required(properties, "merchant.openapi.base-url"))
                .merchantId(required(properties, "merchant.id"))
                .merchantJwtSecret(requiredAny(properties, "merchant.api.private-key", "merchant.jwt.secret"))
                .platformPublicKey(keyValue(properties, "merchant.platform.public-key"))
                .merchantResponsePrivateKey(keyValue(properties, "merchant.response.private-key"))
                .jwtTtlSeconds(PaymentGatewayConstants.JWT_TTL_SECONDS)
                .connectTimeoutMs(PaymentGatewayConstants.HTTP_CONNECT_TIMEOUT_MS)
                .readTimeoutMs(PaymentGatewayConstants.HTTP_READ_TIMEOUT_MS)
                .defaultVersion(PaymentGatewayConstants.DEFAULT_VERSION)
                .build();
    }

    private static Properties loadProperties(String configFileName) {
        String fileName = requiredText(configFileName, "configFileName");
        Properties properties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new PaymentGatewayConfigException("Merchant config file not found: " + fileName);
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new PaymentGatewayConfigException("Failed to load merchant config file: " + fileName, exception);
        }
    }

    private static String required(Properties properties, String key) {
        // 配置缺失时直接报具体 key，避免 SDK 退回到任何硬编码密钥或默认商户号。
        return requiredText(properties.getProperty(key), "Missing required merchant config: " + key);
    }

    private static String requiredAny(Properties properties, String preferredKey, String fallbackKey) {
        String value = properties.getProperty(preferredKey);
        if (value == null || value.trim().isEmpty()) {
            value = properties.getProperty(fallbackKey);
        }
        return requiredText(value, "Missing required merchant config: " + preferredKey + " or " + fallbackKey);
    }

    private static String keyValue(Properties properties, String key) {
        // 平台导出的 PEM 接入包使用 xxx-file；旧版文本配置继续使用 xxx。
        return KeyFileLoader.resolve(properties.getProperty(key + "-file"), properties.getProperty(key), key);
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new PaymentGatewayConfigException(message);
        }
        return value.trim();
    }
}
