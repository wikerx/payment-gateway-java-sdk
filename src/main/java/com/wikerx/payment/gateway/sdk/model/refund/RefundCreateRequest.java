package com.wikerx.payment.gateway.sdk.model.refund;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款创建请求。
 */
@Data
public class RefundCreateRequest {

    /**
     * 退款标识符。
     */
    private String charge;
    /**
     * 平台交易流水号。
     */
    private String tradeNo;
    /**
     * 交易或账户币种，使用 ISO 4217 三位大写币种代码。
     */
    private String currency;
    /**
     * 金额，使用 BigDecimal 表示主币种单位。
     */
    private BigDecimal amount;
    /**
     * 退款金额。
     */
    private BigDecimal refundAmount;
    /**
     * 退款原因。
     */
    private String refundReason;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;
}
