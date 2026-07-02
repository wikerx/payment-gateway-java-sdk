package com.scott.payment.sdk.api.webhook.payout;

import com.scott.payment.sdk.model.webhook.PayoutWebhookRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutWebhookVerifierTest
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代付异步通知签名校验测试，负责验证 SDK 与网关当前 t + tradeNo + currency + amount + status + code + message 的 SHA-256 签名规则一致。
 *                本测试不启动 Web 容器，不发起 HTTP 请求，不修改资金、交易状态或密钥配置。
 * @status : create
 */
class PayoutWebhookVerifierTest {

    /**
     * 验证代付异步通知签名原文和 SHA-256 签名计算。
     */
    @Test
    void sign_shouldBuildGatewayCompatibleSignature() {
        PayoutWebhookVerifier verifier = new PayoutWebhookVerifier();
        PayoutWebhookRequest request = payoutWebhookRequest();

        String signSource = verifier.buildSignSource("1782901024000", request);
        String signature = verifier.sign("1782901024000", request);

        assertThat(signSource).isEqualTo("1782901024000payout_123USD3.111040001003Failed");
        assertThat(signature).isEqualTo("aabd33cd2545e3793b49dbd52b7deba76f346e2f21cd9ce48bcc0b9c93e2330a");
        assertThat(verifier.verify("1782901024000", signature, request)).isTrue();
        assertThat(verifier.verify("1782901024000", "bad-signature", request)).isFalse();
    }

    private PayoutWebhookRequest payoutWebhookRequest() {
        PayoutWebhookRequest request = new PayoutWebhookRequest();
        request.setMerNo("2606177036");
        request.setTradeNo("payout_123");
        request.setOrderNo("PAYOUT_123");
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("3.11"));
        request.setPaymentMethod("CARD");
        request.setStatus(1);
        request.setCode("040001003");
        request.setMessage("Failed");
        request.setMetadata("metadata");
        return request;
    }
}
