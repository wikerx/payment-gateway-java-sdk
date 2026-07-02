package com.scott.payment.sdk.api.webhook;

import com.scott.payment.sdk.api.webhook.payin.PayinWebhookHandler;
import com.scott.payment.sdk.api.webhook.payin.handler.LoggingPayinWebhookHandler;
import com.scott.payment.sdk.api.webhook.payout.PayoutWebhookHandler;
import com.scott.payment.sdk.api.webhook.payout.handler.LoggingPayoutWebhookHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiWebhookConfiguration
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : OpenAPI webhook 示例配置，负责在商户未提供 payin/payout 回调处理器时注册默认日志处理器。
 *                本配置只为 Spring Boot 示例提供可启动的默认 Bean，不落库、不修改资金、不推进交易状态、不投递 MQ。
 *                商户生产接入时应自行提供 PayinWebhookHandler 或 PayoutWebhookHandler Bean 覆盖默认日志处理器。
 * @status : create
 */
@Configuration
public class OpenApiWebhookConfiguration {

    /**
     * 注册代收异步通知默认处理器。
     *
     * 该 Bean 只在商户没有自定义 PayinWebhookHandler 时生效，默认行为仅记录日志，不做业务状态变更。
     *
     * @return 代收异步通知默认日志处理器
     */
    @Bean
    @ConditionalOnMissingBean(PayinWebhookHandler.class)
    public PayinWebhookHandler payinWebhookHandler() {
        return new LoggingPayinWebhookHandler();
    }

    /**
     * 注册代付异步通知默认处理器。
     *
     * 该 Bean 只在商户没有自定义 PayoutWebhookHandler 时生效，默认行为仅记录日志，不做业务状态变更。
     *
     * @return 代付异步通知默认日志处理器
     */
    @Bean
    @ConditionalOnMissingBean(PayoutWebhookHandler.class)
    public PayoutWebhookHandler payoutWebhookHandler() {
        return new LoggingPayoutWebhookHandler();
    }
}
