package com.scott.payment.sdk.model.payment;

import com.scott.payment.sdk.model.common.PaymentMethod;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CardPaymentRequest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 信用卡直连支付请求模型，负责在创建对象时默认设置 paymentMethod=CARD，减少商户示例中手写支付方式代码。
 *                本类只提供信用卡支付方式的本地默认值，不执行卡信息校验、JWT 签名、OpenAPI 报文加密、HTTP 调用、资金扣款或交易状态流转。
 *                paymentMethodData 通常包含卡号和 CVC 等敏感字段，商户侧不得在普通日志中直接输出完整对象。
 * @status : modify
 */
public class CardPaymentRequest extends PaymentCreateRequest {

    /**
     * 创建信用卡直连请求，并默认设置支付方式为 CARD。
     */
    public CardPaymentRequest() {
        setPaymentMethod(PaymentMethod.CARD);
    }
}
