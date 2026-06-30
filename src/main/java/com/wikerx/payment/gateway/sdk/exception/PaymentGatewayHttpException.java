package com.wikerx.payment.gateway.sdk.exception;

/**
 * SDK HTTP 异常，表示网络 I/O 失败、HTTP 非 2xx 或响应体读取失败。
 */
public class PaymentGatewayHttpException extends PaymentGatewayException {

    /**
     * 创建 SDK HTTP 异常。
     *
     * @param message 异常说明
     */
    public PaymentGatewayHttpException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK HTTP 异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public PaymentGatewayHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
