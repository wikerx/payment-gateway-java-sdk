package com.wikerx.payment.gateway.sdk.exception;

/**
 * SDK 配置异常，表示 baseUrl、商户号、密钥、算法或超时配置缺失或非法。
 */
public class PaymentGatewayConfigException extends PaymentGatewayException {

    /**
     * 创建 SDK 配置异常。
     *
     * @param message 异常说明
     */
    public PaymentGatewayConfigException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 配置异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public PaymentGatewayConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
