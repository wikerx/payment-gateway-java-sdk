package com.scott.payment.sdk.model.common;

import lombok.Data;

/**
 * PayPal 支付方式扩展资料。
 */
@Data
public class PaypalPaymentMethodData {

    /**
     * 邮箱地址。
     */
    private String email;
    /**
     * PayPal payer id。
     */
    private String payerId;
}
