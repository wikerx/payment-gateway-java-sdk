package com.wikerx.payment.gateway.sdk.model.refund;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款响应。
 */
@Data
public class RefundResponse {

    /**
     * 平台商户号。
     */
    private String merNo;
    /**
     * 平台交易流水号。
     */
    private String tradeNo;
    /**
     * 商户订单号。
     */
    private String orderNo;
    /**
     * 交易或账户币种，使用 ISO 4217 三位大写币种代码。
     */
    private String currency;
    /**
     * 金额，使用 BigDecimal 表示主币种单位。
     */
    private BigDecimal amount;
    /**
     * 支付方式。
     */
    private String paymentMethod;
    /**
     * 退款标识符。
     */
    private String charge;
    /**
     * 退款金额。
     */
    private BigDecimal refundAmount;
    /**
     * 退款原因。
     */
    private String refundReason;
    /**
     * 交易状态。
     */
    private Integer status;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;
}
