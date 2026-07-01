package com.scott.payment.sdk.exception;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiCryptoException
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 加解密异常，负责表达 RSA-OAEP-256、AES-256-GCM、密钥解析或 compact payload 格式处理失败。
 *                本异常不修改资金、交易或客户状态；异常消息只能说明失败边界，不得包含私钥、完整明文或完整密文。
 * @status : create
 */
public class OpenApiCryptoException extends OpenApiException {

    /**
     * 创建 SDK 加解密异常。
     *
     * @param message 异常说明
     */
    public OpenApiCryptoException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 加解密异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public OpenApiCryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
