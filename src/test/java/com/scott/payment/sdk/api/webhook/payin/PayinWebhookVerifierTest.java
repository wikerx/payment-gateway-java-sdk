package com.scott.payment.sdk.api.webhook.payin;

import com.scott.payment.sdk.model.webhook.PayinWebhookRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinWebhookVerifierTest
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代收异步通知签名校验测试，负责验证 SDK 与网关当前 t + tradeNo + orderNo + currency + amount + status + code + message 的 SHA-256 签名规则一致。
 *                本测试不启动 Web 容器，不发起 HTTP 请求，不修改资金、交易状态或密钥配置。
 * @status : create
 */
class PayinWebhookVerifierTest {

    /**
     * 验证代收异步通知签名原文和 SHA-256 签名计算。
     */
    @Test
    void sign_shouldBuildGatewayCompatibleSignature() {
        PayinWebhookVerifier verifier = new PayinWebhookVerifier();
        PayinWebhookRequest request = payinWebhookRequest();

        String signSource = verifier.buildSignSource("1782901024000", request);
        String signature = verifier.sign("1782901024000", request);

        assertThat(signSource).isEqualTo("1782901024000pay_123ORDER_123USD12.341successPaid");
        assertThat(signature).isEqualTo("8c1dac30edaa495ee6eb55de9b0b7ce94885f7dbabb0d538d96ef1d1ca14987e");
        assertThat(verifier.verify("1782901024000", signature, request)).isTrue();
        assertThat(verifier.verify("1782901024000", "bad-signature", request)).isFalse();
    }

    private PayinWebhookRequest payinWebhookRequest() {
        PayinWebhookRequest request = new PayinWebhookRequest();
        request.setMerNo("2606177036");
        request.setTradeNo("pay_123");
        request.setOrderNo("ORDER_123");
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("12.34"));
        request.setPaymentMethod("CARD");
        request.setStatus(1);
        request.setCode("success");
        request.setMessage("Paid");
        request.setMetadata("metadata");
        return request;
    }
}
