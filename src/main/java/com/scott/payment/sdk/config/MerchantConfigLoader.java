package com.scott.payment.sdk.config;

import com.scott.payment.sdk.PaymentGatewayClientConfig;
import com.scott.payment.sdk.exception.PaymentGatewayConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : MerchantConfigLoader
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 商户配置加载器，负责从 classpath 固定读取 merchant-config.properties，并封装为 SDK 运行配置。
 *                本类不支持按商户号动态切换配置文件，避免商户示例项目中混入多套测试商户数据；密钥字段只加载到内存，不输出到日志。
 * @status : modify
 */
public final class MerchantConfigLoader {

    private MerchantConfigLoader() {
    }

    /**
     * 加载默认商户配置文件。
     *
     * 该方法只读取 SDK 约定的 merchant-config.properties，并会同时解析 classpath 密钥文件或内联密钥文本。
     * 方法不会修改资金、交易状态、密钥文件或远程配置；配置缺失或格式错误时抛出 SDK 配置异常。
     *
     * @return SDK 客户端配置
     */
    public static PaymentGatewayClientConfig load() {
        Properties properties = loadProperties();
        return PaymentGatewayClientConfig.builder()
                .baseUrl(required(properties, "payment.gateway.base-url"))
                .merchantId(required(properties, "payment.gateway.merchant-no"))
                .livemode(requiredBoolean(properties, "payment.gateway.livemode"))
                .merchantJwtSecret(required(properties, "payment.gateway.api-private-key"))
                .platformPublicKey(keyValue(properties,
                        "payment.gateway.platform-request-public-key-path",
                        "payment.gateway.platform-request-public-key"))
                .merchantResponsePrivateKey(keyValue(properties,
                        "payment.gateway.merchant-response-private-key-path",
                        "payment.gateway.merchant-response-private-key"))
                .jwtTtlSeconds(PaymentGatewayConstants.JWT_TTL_SECONDS)
                .connectTimeoutMs(PaymentGatewayConstants.HTTP_CONNECT_TIMEOUT_MS)
                .readTimeoutMs(PaymentGatewayConstants.HTTP_READ_TIMEOUT_MS)
                .defaultVersion(PaymentGatewayConstants.DEFAULT_VERSION)
                .rawHttpLogEnabled(optionalBoolean(properties, "payment.gateway.debug-raw-log-enabled", Boolean.FALSE))
                .build();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(PaymentGatewayConstants.CONFIG_FILE_NAME)) {
            if (inputStream == null) {
                throw new PaymentGatewayConfigException("Merchant config file not found: "
                        + PaymentGatewayConstants.CONFIG_FILE_NAME);
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new PaymentGatewayConfigException("Failed to load merchant config file: "
                    + PaymentGatewayConstants.CONFIG_FILE_NAME, exception);
        }
    }

    private static String required(Properties properties, String key) {
        return requiredText(properties.getProperty(key), "Missing required merchant config: " + key);
    }

    private static Boolean requiredBoolean(Properties properties, String key) {
        String value = required(properties, key);
        return parseBoolean(value, key);
    }

    private static Boolean optionalBoolean(Properties properties, String key, Boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return parseBoolean(value, key);
    }

    private static Boolean parseBoolean(String value, String key) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new PaymentGatewayConfigException("Merchant config " + key + " must be true or false");
    }

    private static String keyValue(Properties properties, String pathKey, String inlineKey) {
        // 文件路径优先；未配置路径时也允许商户直接粘贴 PEM/Base64 文本。
        String keyLocation = properties.getProperty(pathKey);
        return KeyFileLoader.resolve(keyLocation, properties.getProperty(inlineKey), inlineKey);
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new PaymentGatewayConfigException(message);
        }
        return value.trim();
    }
}
