package com.scott.payment.sdk.model.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentResponse
 * @date : 2026-07-02 14:18
 * @email : scott_x@163.com
 * @description : 代收交易响应模型，负责承载网关返回的代收交易标识、金额、支付方式、状态码和跳转资料。
 *                本类只做响应字段承载和状态枚举映射，不执行响应解密、资金扣款、回调处理、幂等落库或交易状态推进。
 *                amount 涉及资金金额，email、clientSecret、redirectUrl 等字段可能涉及商户业务敏感信息，日志输出前应按商户安全要求处理。
 * @status : modify
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

    /**
     * 获取代收交易状态枚举。
     *
     * 该方法只根据响应 status 做 SDK 本地映射，不访问网关、不修改资金或交易状态；未知状态返回 UNKNOWN，方便商户兼容新增状态。
     *
     * @return 代收交易状态枚举
     */
    @JsonIgnore
    public PaymentTradeStatus getStatusEnum() {
        return PaymentTradeStatus.fromStatus(status);
    }

    /**
     * 获取代收交易状态说明。
     *
     * 该方法用于商户联调日志展示，不参与签名、加密、对账或状态流转。
     *
     * @return 状态说明
     */
    @JsonIgnore
    public String getStatusDescription() {
        return getStatusEnum().getMessage();
    }
}
