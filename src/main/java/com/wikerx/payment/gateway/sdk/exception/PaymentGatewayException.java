package com.wikerx.payment.gateway.sdk.exception;

/**
 * SDK 根异常，表示本地配置、签名、加密、HTTP 调用或响应解析失败。
 */
public class PaymentGatewayException extends RuntimeException {

    /**
     * 创建 SDK 异常。
     *
     * @param message 异常说明
     */
    public PaymentGatewayException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
