package com.wikerx.payment.gateway.sdk.exception;

/**
 * SDK 加解密异常，表示 RSA、AES-GCM、密钥解析或密文格式处理失败。
 */
public class PaymentGatewayCryptoException extends PaymentGatewayException {

    /**
     * 创建 SDK 加解密异常。
     *
     * @param message 异常说明
     */
    public PaymentGatewayCryptoException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 加解密异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public PaymentGatewayCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
