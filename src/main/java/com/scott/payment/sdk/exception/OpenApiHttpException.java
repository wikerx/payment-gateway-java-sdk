package com.scott.payment.sdk.exception;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiHttpException
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI SDK HTTP 异常，负责表达网络 I/O 失败、HTTP 非 2xx 或响应体读取失败。
 *                本异常只说明传输层调用失败，不判断网关是否已处理业务；商户对支付、退款、代付类请求需按自身订单号做幂等查询确认。
 * @status : create
 */
public class OpenApiHttpException extends OpenApiException {

    /**
     * 创建 SDK HTTP 异常。
     *
     * @param message 异常说明
     */
    public OpenApiHttpException(String message) {
        super(message);
    }

    /**
     * 创建带原始异常的 SDK HTTP 异常。
     *
     * @param message 异常说明
     * @param cause   原始异常
     */
    public OpenApiHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
