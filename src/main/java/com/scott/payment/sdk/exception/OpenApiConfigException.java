package com.scott.payment.sdk.exception;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiConfigException
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 配置异常，负责表达 baseUrl、商户号、livemode、JWT 密钥、RSA 密钥或超时配置缺失和非法。
 *                本异常发生在请求发起前，不涉及资金修改、状态流转或外部渠道调用；异常消息不得包含密钥明文。
 * @status : create
 */
public class OpenApiConfigException extends OpenApiException {

    /**
     * 创建 SDK 配置异常。
     *
     * @param message 异常说明
     */
    public OpenApiConfigException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 配置异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public OpenApiConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
