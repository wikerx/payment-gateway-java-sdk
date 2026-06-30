package com.wikerx.payment.gateway.sdk.model.common;

import lombok.Data;
import lombok.ToString;

/**
 * 信用卡直连支付资料。仅可在商户服务端使用，禁止进入日志。
 */
@Data
public class CardPaymentMethodData {

    /**
     * 卡号，禁止进入日志。
     */
    @ToString.Exclude
    private String number;

    /**
     * 卡有效期月份。
     */
    private String expMonth;
    /**
     * 卡有效期年份。
     */
    private String expYear;

    /**
     * 卡安全码，禁止进入日志。
     */
    @ToString.Exclude
    private String cvc;

    /**
     * 持卡人姓名。
     */
    private String holderName;
    /**
     * 卡品牌。
     */
    private String brand;
}
