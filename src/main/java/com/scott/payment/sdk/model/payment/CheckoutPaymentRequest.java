package com.scott.payment.sdk.model.payment;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CheckoutPaymentRequest
 * @date : 2026-07-02 14:18
 * @email : scott_x@163.com
 * @description : 收银台代收请求模型，负责承载商户创建收银台支付时提交的订单、金额、客户和回跳地址等参数。
 *                本类只作为 PaymentCreateRequest 的语义化子类，不执行签名、OpenAPI 报文加密、HTTP 调用、资金扣款、
 *                支付状态确认或回调处理；最终交易状态应以查询接口或网关异步通知为准。
 * @status : modify
 */
public class CheckoutPaymentRequest extends PaymentCreateRequest {
}
