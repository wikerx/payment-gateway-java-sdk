package com.scott.payment.sdk.api.webhook.v2;

import lombok.Builder;
import lombok.Data;

/**
 * 已完成验签的商户回调 V2 JWT Claims。
 *
 * Claims 只保存回调安全校验所需的最小字段，业务明细以加密 body.data 为准。
 */
@Data
@Builder
public class WebhookV2Claims {

    /**
     * 商户号，必须与 SDK 配置一致。
     */
    private String merchantId;

    /**
     * 环境标识，必须与 SDK 配置和 X-Livemode 一致。
     */
    private Boolean livemode;

    /**
     * 网关回调事件号。
     */
    private String eventId;

    /**
     * 回调事件类型，代收为 PAYIN_CALLBACK，代付为 PAYOUT_CALLBACK。
     */
    private String eventType;

    /**
     * 平台交易号，必须与解密后的业务报文 tradeNo 一致。
     */
    private String tradeNo;

    /**
     * JWT 唯一编号，当前与 eventId 保持一致。
     */
    private String jti;
}
