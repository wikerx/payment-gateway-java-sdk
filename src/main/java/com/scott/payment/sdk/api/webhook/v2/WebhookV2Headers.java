package com.scott.payment.sdk.api.webhook.v2;

import lombok.Builder;
import lombok.Data;

/**
 * 商户回调 V2 HTTP Header。
 *
 * 网关推送 V2 回调时携带 Authorization JWT、环境、回调版本和事件号。
 * 商户号只从 JWT merchantId claim 获取，避免在普通 Header 中重复暴露。
 */
@Data
@Builder
public class WebhookV2Headers {

    /**
     * 网关生成的 Bearer JWT，日志中不得输出完整值。
     */
    private String authorization;

    /**
     * 环境标识，对应商户配置 livemode。
     */
    private String livemode;

    /**
     * 回调版本，V2 加密回调固定为 v2。
     */
    private String callbackVersion;

    /**
     * 回调事件号，对应 JWT claim eventId/jti，用于追踪和幂等。
     */
    private String callbackEventId;
}
