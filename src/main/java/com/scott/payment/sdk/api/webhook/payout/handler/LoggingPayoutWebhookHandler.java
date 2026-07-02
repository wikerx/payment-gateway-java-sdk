package com.scott.payment.sdk.api.webhook.payout.handler;

import com.scott.payment.sdk.api.webhook.payout.PayoutWebhookHandler;
import com.scott.payment.sdk.model.webhook.PayoutWebhookRequest;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : LoggingPayoutWebhookHandler
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代付异步通知默认日志处理器，负责在商户未自定义处理器时输出验签通过后的回调摘要。
 *                本类不落库、不修改资金、不推进交易状态、不投递 MQ，也不保证生产幂等；只用于 SDK 示例和本地联调。
 *                商户生产环境应提供自己的 PayoutWebhookHandler Bean，按 tradeNo/orderNo 做幂等、终态保护和对账。
 * @status : create
 */
@Slf4j
public class LoggingPayoutWebhookHandler implements PayoutWebhookHandler {

    /**
     * 记录已验签的代付异步通知。
     *
     * 该方法只写日志，不开启事务、不写数据库、不修改资金或订单状态。
     *
     * @param request 代付回调参数
     */
    @Override
    public void handle(PayoutWebhookRequest request) {
        log.info("代付异步通知-默认处理器收到回调: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
    }
}
