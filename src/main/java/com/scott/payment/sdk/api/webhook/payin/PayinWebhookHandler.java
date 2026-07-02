package com.scott.payment.sdk.api.webhook.payin;

import com.scott.payment.sdk.model.webhook.PayinWebhookRequest;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinWebhookHandler
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代收异步通知业务处理接口，负责给商户项目承接验签通过后的回调结果处理扩展点。
 *                SDK 默认实现只记录日志，不落库、不修改资金、不推进交易状态、不做 MQ 投递。
 *                商户生产接入时必须自行实现该接口，并基于 tradeNo 或 orderNo 做幂等、终态保护和资金对账。
 * @status : create
 */
public interface PayinWebhookHandler {

    /**
     * 处理已通过签名校验的代收异步通知。
     *
     * 该方法由 Controller 在验签成功后调用；SDK 不开启事务，是否落库、是否修改订单状态、是否投递 MQ 由商户实现决定。
     * 生产实现必须保证幂等，避免网关重试通知导致重复入账、重复状态推进或终态覆盖。
     *
     * @param request 代收回调参数
     */
    void handle(PayinWebhookRequest request);
}
