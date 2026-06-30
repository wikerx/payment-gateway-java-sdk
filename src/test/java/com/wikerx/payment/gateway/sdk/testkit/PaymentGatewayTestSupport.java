package com.wikerx.payment.gateway.sdk.testkit;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.config.MerchantConfigLoader;

import java.math.BigDecimal;

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
     * 返回测试 livemode。
     */
    public static Boolean livemode() {
        return clientConfig().getLivemode();
    }

    /**
     * 创建测试金额。
     */
    public static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
