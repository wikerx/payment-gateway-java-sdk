package com.scott.payment.sdk.model.balance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户余额响应。
 */
@Data
public class BalanceResponse {

    /**
     * 平台商户号。
     */
    private String merNo;
    /**
     * 交易或账户币种，使用 ISO 4217 三位大写币种代码。
     */
    private String currency;
    /**
     * 冻结金额。
     */
    private BigDecimal frozenAmounts;
    /**
     * 可用余额。
     */
    private BigDecimal balance;
    /**
     * 已提现总金额。
     */
    private BigDecimal withdrawnAmounts;
}
