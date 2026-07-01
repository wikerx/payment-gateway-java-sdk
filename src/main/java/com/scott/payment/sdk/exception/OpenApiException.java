package com.scott.payment.sdk.exception;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiException
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 根异常，负责统一表达本地配置、签名、加密、HTTP 调用或响应解析失败。
 *                本异常不代表网关已经修改资金或交易状态；业务失败应优先读取 OpenApiResult 的 code 和 msg。
 *                异常消息不得拼接 JWT、私钥、完整密文、卡号或 CVC 等敏感数据。
 * @status : create
 */
public class OpenApiException extends RuntimeException {

    /**
     * 创建 SDK 异常。
     *
     * @param message 异常说明
     */
    public OpenApiException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public OpenApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
