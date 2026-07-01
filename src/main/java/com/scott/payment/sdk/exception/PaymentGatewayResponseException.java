package com.scott.payment.sdk.exception;

/**
 * SDK 响应异常，表示响应 JSON 非法、响应外壳不符合协议或响应 data 解密解析失败。
 */
public class PaymentGatewayResponseException extends PaymentGatewayException {

    /**
     * 创建 SDK 响应异常。
     *
     * @param message 异常说明
     */
    public PaymentGatewayResponseException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 响应异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public PaymentGatewayResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
