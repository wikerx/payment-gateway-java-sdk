package com.wikerx.payment.gateway.sdk.exception;

/**
 * SDK 基础参数校验异常，表示调用方传入的请求对象缺少必要字段。
 */
public class PaymentGatewayValidationException extends PaymentGatewayException {

    /**
     * 创建 SDK 基础参数校验异常。
     *
     * @param message 异常说明
     */
    public PaymentGatewayValidationException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 基础参数校验异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public PaymentGatewayValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
