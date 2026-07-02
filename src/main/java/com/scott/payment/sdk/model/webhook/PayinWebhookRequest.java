package com.scott.payment.sdk.model.webhook;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinWebhookRequest
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代收异步通知请求模型，负责承载网关通过 notifyUrl 以 GET form 参数回传的代收交易结果。
 *                本类只描述回调参数，不执行验签、幂等落库、资金入账、状态流转或外部渠道调用。
 *                amount 涉及资金金额，tradeNo/orderNo 用于商户侧幂等，status/code/message 表示网关侧处理结果。
 * @status : create
 */
@Data
public class PayinWebhookRequest {

    /**
     * 商户号。
     *
     * 敏感字段：否。
     * 是否允许为空：以网关回调实际参数为准。
     * 用途：商户侧核对回调所属商户账户。
     */
    private String merNo;

    /**
     * 网关代收交易号。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名、幂等处理、交易查询和问题排查。
     */
    private String tradeNo;

    /**
     * 商户订单号。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名和商户侧订单幂等关联。
     */
    private String orderNo;

    /**
     * 交易币种，ISO 4217 三位大写币种代码。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名和资金核对。
     */
    private String currency;

    /**
     * 代收金额，主币种单位。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名、订单金额核验和对账。
     */
    private BigDecimal amount;

    /**
     * 支付方式。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     * 用途：商户侧区分 CARD、CHECKOUT、本地支付等支付方式。
     */
    private String paymentMethod;

    /**
     * 交易时间。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     * 格式：以网关回调实际字符串为准。
     */
    private String tradeDate;

    /**
     * 支付状态。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名和商户侧状态流转判断。
     * 限制：商户侧必须做终态保护，避免旧回调覆盖新状态。
     */
    private Integer status;

    /**
     * 网关状态代码。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名，辅助商户识别支付结果。
     */
    private String code;

    /**
     * 网关状态消息。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名，辅助商户识别支付结果。
     */
    private String message;

    /**
     * 重定向地址。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     * 用途：部分代收方式返回收银台或三方支付跳转地址。
     */
    private String redirectUrl;

    /**
     * 客户端密钥。
     *
     * 敏感字段：是。
     * 是否允许为空：允许为空。
     * 用途：部分前端支付方式完成客户端确认流程。
     * 限制：不得写入普通日志或暴露给无关系统。
     */
    private String clientSecret;

    /**
     * 过期时间。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     * 格式：以网关回调实际字符串为准。
     */
    private String expireTime;

    /**
     * 商户透传字段。
     *
     * 敏感字段：取决于商户写入内容。
     * 是否允许为空：允许为空。
     * 限制：不参与当前代收回调签名，不建议写入敏感个人信息。
     */
    private String metadata;

    /**
     * 备注。
     *
     * 敏感字段：取决于商户写入内容。
     * 是否允许为空：允许为空。
     * 限制：不参与当前代收回调签名。
     */
    private String remark;

    /**
     * 邮箱。
     *
     * 敏感字段：是。
     * 是否允许为空：允许为空。
     * 用途：用于商户侧客户识别或支付结果通知展示，日志中应脱敏。
     */
    private String email;

    /**
     * 客户姓名。
     *
     * 敏感字段：是。
     * 是否允许为空：允许为空。
     * 用途：用于商户侧客户识别或支付结果展示。
     */
    private String name;

    /**
     * 事件类型。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     * 用途：订阅或特殊支付事件区分。
     */
    private String eventType;

    /**
     * 订阅类型，0 表示首次，1 表示续订。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     */
    private Integer subType;

    /**
     * 订阅周期。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     */
    private Integer subscriptionMode;

    /**
     * 订阅 token。
     *
     * 敏感字段：是。
     * 是否允许为空：允许为空。
     * 用途：订阅续扣场景关联订阅关系，不参与当前回调签名。
     */
    private String subToken;

    /**
     * 通道名称。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     * 用途：白名单商户排查通道侧处理结果。
     */
    private String channelCode;

    /**
     * 上游订单 ID。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     * 用途：白名单商户关联上游渠道订单。
     */
    private String channelId;
}
