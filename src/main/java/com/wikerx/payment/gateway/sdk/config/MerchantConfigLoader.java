package com.wikerx.payment.gateway.sdk.config;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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
     * 加载指定配置文件，适用于测试或同一应用内多商户场景。
     * <p>
     * 优先从文件系统读取绝对路径或相对路径；文件不存在时再从 classpath 读取。
     *
     * @param configFileName classpath、绝对路径或相对路径配置文件名
     * @return SDK 客户端配置
     */
    public static PaymentGatewayClientConfig load(String configFileName) {
        Properties properties = loadProperties(configFileName);
        return PaymentGatewayClientConfig.builder()
                .baseUrl(requiredAny(properties, "payment.gateway.base-url", "merchant.openapi.base-url"))
                .merchantId(requiredAny(properties, "payment.gateway.merchant-no", "merchant.id"))
                .livemode(requiredBooleanAny(properties, "payment.gateway.livemode", "merchant.livemode"))
                .merchantJwtSecret(requiredAny(properties, "payment.gateway.api-private-key", "merchant.api.private-key", "merchant.jwt.secret"))
                .platformPublicKey(keyValue(properties,
                        "payment.gateway.platform-request-public-key-path",
                        "merchant.platform.public-key-file",
                        "merchant.platform.public-key"))
                .merchantResponsePrivateKey(keyValue(properties,
                        "payment.gateway.merchant-response-private-key-path",
                        "merchant.response.private-key-file",
                        "merchant.response.private-key"))
                .jwtTtlSeconds(PaymentGatewayConstants.JWT_TTL_SECONDS)
                .connectTimeoutMs(optionalPositiveInteger(properties, PaymentGatewayConstants.HTTP_CONNECT_TIMEOUT_MS,
                        "payment.gateway.connect-timeout-ms", "merchant.http.connect-timeout-ms"))
                .readTimeoutMs(optionalPositiveInteger(properties, PaymentGatewayConstants.HTTP_READ_TIMEOUT_MS,
                        "payment.gateway.read-timeout-ms", "merchant.http.read-timeout-ms"))
                .defaultVersion(PaymentGatewayConstants.DEFAULT_VERSION)
                .build();
    }

    private static Properties loadProperties(String configFileName) {
        String fileName = requiredText(configFileName, "configFileName");
        Properties properties = new Properties();
        if (Files.exists(Paths.get(fileName))) {
            try (InputStream inputStream = Files.newInputStream(Paths.get(fileName))) {
                properties.load(inputStream);
                return properties;
            } catch (IOException exception) {
                throw new PaymentGatewayConfigException("Failed to load merchant config file: " + fileName, exception);
            }
        }
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

    private static String requiredAny(Properties properties, String preferredKey, String fallbackKey) {
        String value = properties.getProperty(preferredKey);
        if (value == null || value.trim().isEmpty()) {
            value = properties.getProperty(fallbackKey);
        }
        return requiredText(value, "Missing required merchant config: " + preferredKey + " or " + fallbackKey);
    }

    private static String requiredAny(Properties properties, String firstKey, String secondKey, String thirdKey) {
        String value = properties.getProperty(firstKey);
        if (value == null || value.trim().isEmpty()) {
            value = properties.getProperty(secondKey);
        }
        if (value == null || value.trim().isEmpty()) {
            value = properties.getProperty(thirdKey);
        }
        return requiredText(value, "Missing required merchant config: " + firstKey + ", " + secondKey + " or " + thirdKey);
    }

    private static Boolean requiredBooleanAny(Properties properties, String preferredKey, String fallbackKey) {
        String value = requiredAny(properties, preferredKey, fallbackKey);
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        throw new PaymentGatewayConfigException("Merchant config " + preferredKey + " or " + fallbackKey + " must be true or false");
    }

    private static String keyValue(Properties properties, String pathKey, String legacyFileKey, String inlineKey) {
        // 新配置使用 xxx-path；旧版文本配置继续使用 xxx，文件配置优先于文本配置。
        String keyLocation = properties.getProperty(pathKey);
        if (keyLocation == null || keyLocation.trim().isEmpty()) {
            keyLocation = properties.getProperty(legacyFileKey);
        }
        return KeyFileLoader.resolve(keyLocation, properties.getProperty(inlineKey), inlineKey);
    }

    private static Integer optionalPositiveInteger(Properties properties, int defaultValue, String preferredKey, String fallbackKey) {
        String value = properties.getProperty(preferredKey);
        if (value == null || value.trim().isEmpty()) {
            value = properties.getProperty(fallbackKey);
        }
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int integerValue = Integer.parseInt(value.trim());
            if (integerValue <= 0) {
                throw new NumberFormatException("not positive");
            }
            return integerValue;
        } catch (NumberFormatException exception) {
            throw new PaymentGatewayConfigException("Merchant config " + preferredKey + " or " + fallbackKey
                    + " must be a positive integer", exception);
        }
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new PaymentGatewayConfigException(message);
        }
        return value.trim();
    }
}
