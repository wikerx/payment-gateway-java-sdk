package com.wikerx.payment.gateway.sdk.testkit;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.config.MerchantConfigLoader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Properties;

/**
 * SDK 单元测试支撑工具。
 */
public final class PaymentGatewayTestSupport {

    private PaymentGatewayTestSupport() {
    }

    /**
     * 加载测试客户端配置。
     */
    public static PaymentGatewayClientConfig clientConfig() {
        return MerchantConfigLoader.load("merchant-config.properties");
    }

    /**
     * 加载测试服务端密钥材料。
     */
    public static Properties serviceKeyMaterial() {
        Properties properties = new Properties();
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("openapi-test-key-material.properties")) {
            properties.load(inputStream);
            return properties;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * 返回测试商户号。
     */
    public static String merchantId() {
        return clientConfig().getMerchantId();
    }

    /**
     * 返回测试 JWT 密钥。
     */
    public static String merchantJwtSecret() {
        return clientConfig().getMerchantJwtSecret();
    }

    /**
     * 创建测试金额。
     */
    public static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
