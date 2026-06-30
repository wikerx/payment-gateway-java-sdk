package com.wikerx.payment.gateway.sdk.model.payout;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 代付交易响应。
 */
@Data
public class PayoutResponse {

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
     * 完成时间。
     */
    private String completionDate;
    /**
     * 交易状态。
     */
    private Integer status;
    /**
     * 业务状态码。
     */
    private String code;
    /**
     * 业务状态说明。
     */
    private String message;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;
}
