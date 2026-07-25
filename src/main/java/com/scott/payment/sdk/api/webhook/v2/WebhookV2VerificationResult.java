package com.scott.payment.sdk.api.webhook.v2;

/**
 * 商户回调 V2 验签解密结果。
 *
 * @param <T> 解密后的业务回调类型
 */
public class WebhookV2VerificationResult<T> {

    private final WebhookV2Claims claims;
    private final T payload;

    public WebhookV2VerificationResult(WebhookV2Claims claims, T payload) {
        this.claims = claims;
        this.payload = payload;
    }

    /**
     * 返回已经验签通过的 JWT Claims。
     *
     * @return V2 回调 Claims
     */
    public WebhookV2Claims getClaims() {
        return claims;
    }

    /**
     * 返回解密并解析后的业务回调报文。
     *
     * @return 业务回调报文
     */
    public T getPayload() {
        return payload;
    }
}
