package com.scott.payment.sdk.model.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 代收交易响应。
 */
@Data
public class PaymentResponse {

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
     * 可用支付方式集合。
     */
    private Set<String> paymentMethodTypes;
    /**
     * 支付方式。
     */
    private String paymentMethod;
    /**
     * 交易时间。
     */
    private String tradeDate;
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
     * 支付跳转地址。
     */
    private String redirectUrl;
    /**
     * 客户端密钥。
     */
    private String clientSecret;
    /**
     * 过期时间。
     */
    private String expireTime;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;
    /**
     * 邮箱地址。
     */
    private String email;
    /**
     * 姓名。
     */
    private String name;
    /**
     * 事件类型。
     */
    private String eventType;
    /**
     * 订阅类型。
     */
    private Integer subType;
    /**
     * 订阅周期。
     */
    private Integer subscriptionMode;
    /**
     * 订阅 token。
     */
    private String subToken;
    /**
     * 通道代码。
     */
    private String channelCode;
    /**
     * 上游订单 ID。
     */
    private String channelId;
}
