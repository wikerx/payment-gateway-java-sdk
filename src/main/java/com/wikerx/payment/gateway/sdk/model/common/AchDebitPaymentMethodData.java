package com.wikerx.payment.gateway.sdk.model.common;

import lombok.Data;
import lombok.ToString;

/**
 * ACH Debit 支付方式扩展资料。
 */
@Data
public class AchDebitPaymentMethodData {

    /**
     * 账户持有人姓名。
     */
    private String accountHolderName;
    /**
     * 银行 routing number。
     */
    private String routingNumber;

    /**
     * 银行账号，禁止进入日志。
     */
    @ToString.Exclude
    private String bankAccountNo;

    /**
     * 银行账户类型。
     */
    private String accountType;
}
