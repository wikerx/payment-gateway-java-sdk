package com.scott.payment.sdk.exception;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiResponseException
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 响应异常，负责表达响应 JSON 非法、响应外壳不符合协议、livemode 不一致或响应 data 解密解析失败。
 *                本异常不修改资金状态；涉及支付、退款、代付终态确认时，商户应使用查询接口核验服务端最终结果。
 * @status : create
 */
public class OpenApiResponseException extends OpenApiException {

    /**
     * 创建 SDK 响应异常。
     *
     * @param message 异常说明
     */
    public OpenApiResponseException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK 响应异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public OpenApiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
