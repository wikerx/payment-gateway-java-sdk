package com.scott.payment.sdk.model.payment;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : LocalPaymentRequest
 * @date : 2026-07-02 14:18
 * @email : scott_x@163.com
 * @description : 本地支付直连代收请求模型，负责承载 payType=1 场景下商户后端直接提交的支付方式和支付资料。
 *                本类只作为 PaymentCreateRequest 的语义化子类，不默认设置 paymentMethod，不执行签名、OpenAPI 报文加密、
 *                HTTP 调用、资金扣款、支付资料保存或交易状态流转；paymentMethodData 可能包含卡号、CVC 或本地支付账户等敏感资料。
 * @status : modify
 */
public class LocalPaymentRequest extends PaymentCreateRequest {
}
