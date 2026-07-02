package com.scott.payment.sdk.model.webhook;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutWebhookRequest
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代付异步通知请求模型，负责承载网关通过 notifyUrl 以 GET form 参数回传的代付交易结果。
 *                本类只描述回调参数，不执行验签、幂等落库、资金入账、状态流转或外部渠道调用。
 *                amount 涉及资金金额，status、code、message 表示网关侧最终处理结果，商户处理时必须结合 tradeNo/orderNo 做幂等保护。
 * @status : create
 */
@Data
public class PayoutWebhookRequest {

    /**
     * 商户号。
     *
     * 敏感字段：否。
     * 是否允许为空：以网关回调实际参数为准。
     * 用途：商户侧可用于核对回调来源账户。
     */
    private String merNo;

    /**
     * 网关代付交易号。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调幂等、交易查询和问题排查。
     */
    private String tradeNo;

    /**
     * 商户订单号。
     *
     * 敏感字段：否。
     * 是否允许为空：以网关回调实际参数为准。
     * 用途：商户侧订单幂等和业务单据关联。
     */
    private String orderNo;

    /**
     * 交易币种，ISO 4217 三位大写币种代码。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与资金核对和回调签名摘要。
     */
    private String currency;

    /**
     * 代付金额，主币种单位。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与资金核对和回调签名摘要。
     */
    private BigDecimal amount;

    /**
     * 支付方式代码。
     *
     * 敏感字段：否。
     * 是否允许为空：以网关回调实际参数为准。
     * 用途：商户侧可用于区分 CARD、PAY_PAL、CASHAPP、UPI、ACH_DEBIT 等代付方式。
     */
    private String paymentMethod;

    /**
     * 代付完成时间。
     *
     * 敏感字段：否。
     * 是否允许为空：允许为空。
     * 格式：以网关回调实际字符串为准。
     */
    private String completionDate;

    /**
     * 代付状态。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名摘要和商户侧状态流转判断。
     * 限制：商户侧必须做终态保护，避免旧回调覆盖新状态。
     */
    private Integer status;

    /**
     * 网关状态代码。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名摘要，辅助商户识别失败原因。
     */
    private String code;

    /**
     * 网关状态消息。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与回调签名摘要，辅助商户识别失败原因。
     */
    private String message;

    /**
     * 商户透传字段。
     *
     * 敏感字段：取决于商户写入内容。
     * 是否允许为空：允许为空。
     * 限制：不参与当前代付回调签名，不建议写入敏感个人信息。
     */
    private String metadata;

    /**
     * 备注。
     *
     * 敏感字段：取决于商户写入内容。
     * 是否允许为空：允许为空。
     * 限制：不参与当前代付回调签名，不建议写入敏感个人信息。
     */
    private String remark;
}
