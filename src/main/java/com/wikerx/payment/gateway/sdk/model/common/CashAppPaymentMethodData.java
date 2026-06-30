package com.wikerx.payment.gateway.sdk.model.common;

import lombok.Data;

/**
 * Cash App 支付方式扩展资料。
 */
@Data
public class CashAppPaymentMethodData {

    /**
     * Cash App cashtag。
     */
    private String cashtag;
    /**
     * 邮箱地址。
     */
    private String email;
}
