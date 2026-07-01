package com.scott.payment.sdk.exception;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiValidationException
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 请求参数校验异常，负责表达调用方传入的请求对象为空或必要字段缺失。
 *                本异常发生在签名、加密和 HTTP 调用前，不涉及资金变更、状态流转或外部渠道调用。
 * @status : create
 */
public class OpenApiValidationException extends OpenApiException {

    /**
     * 创建 SDK 基础参数校验异常。
     *
     * @param message 异常说明
     */
    public OpenApiValidationException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 基础参数校验异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public OpenApiValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
