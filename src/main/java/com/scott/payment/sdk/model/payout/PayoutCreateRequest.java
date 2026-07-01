package com.scott.payment.sdk.model.payout;

import com.scott.payment.sdk.model.common.CustomerInfo;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 代付创建请求。
 */
@Data
public class PayoutCreateRequest {

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
     * 客户端 IP。
     */
    private String clientIp;
    /**
     * 商户网站地址。
     */
    private String website;
    /**
     * 异步通知地址。
     */
    private String notifyUrl;
    /**
     * 关联子商户号。
     */
    private String onBehalfOf;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;
    /**
     * 客户 ID。
     */
    private String customerId;
    /**
     * 客户资料。
     */
    private CustomerInfo customer;
    /**
     * 可用支付方式集合。
     */
    private Set<String> paymentMethodTypes;
    /**
     * 支付方式。
     */
    private String paymentMethod;

    /**
     * 支付方式扩展数据，可能包含敏感信息。
     */
    @ToString.Exclude
    private Map<String, Object> paymentMethodData;
}
