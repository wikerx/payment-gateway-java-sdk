package com.scott.payment.sdk.model.payment;

/**
 * 信用卡直连请求。构造后默认设置 paymentMethod=CARD。
 */
public class CardPaymentRequest extends PaymentCreateRequest {

    /**
     * 创建信用卡直连请求，并默认设置支付方式为 CARD。
     */
    public CardPaymentRequest() {
        setPaymentMethod("CARD");
    }
}
