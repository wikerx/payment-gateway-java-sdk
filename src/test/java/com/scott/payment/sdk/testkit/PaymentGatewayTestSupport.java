package com.scott.payment.sdk.testkit;

import com.scott.payment.sdk.PaymentGatewayClientConfig;
import com.scott.payment.sdk.config.MerchantConfigLoader;

import java.math.BigDecimal;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentGatewayTestSupport
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : SDK 测试支撑工具，负责加载 2606177036 沙盒商户配置并提供金额构造等测试辅助方法。
 *                本类不新增数据库测试数据，不发起真实外部渠道调用；返回的 API 密钥仅用于本地构造 JWT，测试日志不得输出密钥明文。
 * @status : modify
 */
public final class PaymentGatewayTestSupport {

    private PaymentGatewayTestSupport() {
    }

    /**
     * 加载测试客户端配置。
     *
     * @return 2606177036 沙盒商户 SDK 配置
     */
    public static PaymentGatewayClientConfig clientConfig() {
        return MerchantConfigLoader.load();
    }

    /**
     * 返回测试商户号。
     *
     * @return 沙盒商户号
     */
    public static String merchantId() {
        return clientConfig().getMerchantId();
    }

    /**
     * 返回测试 JWT 密钥。
     *
     * @return JWT HS256 API 私钥，调用方不得写入日志
     */
    public static String merchantJwtSecret() {
        return clientConfig().getMerchantJwtSecret();
    }

    /**
     * 返回测试 livemode。
     *
     * @return false 表示沙盒，true 表示生产
     */
    public static Boolean livemode() {
        return clientConfig().getLivemode();
    }

    /**
     * 创建测试金额。
     *
     * @param value 十进制金额字符串，单位为对应币种主单位
     * @return 精确金额对象
     */
    public static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
