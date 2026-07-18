package com.scott.payment.sdk.api.webhook.payin;

import com.scott.payment.sdk.model.webhook.PayinWebhookRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

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

    /**
     * 验证代收回调验签保留网关原始金额字符串。
     *
     * 网关回调 URL 中 amount=19.00 时，签名原文必须使用 19.00，不能按数值转换成 19。
     */
    @Test
    void verify_withRawAmountScale_shouldKeepGatewayAmountText() {
        PayinWebhookVerifier verifier = new PayinWebhookVerifier();
        Map<String, String> params = payinWebhookParams("19.00");
        String timestamp = "1784111725000";

        String signSource = verifier.buildSignSource(timestamp, params);
        String signature = verifier.sign(timestamp, params);

        assertThat(signSource).isEqualTo("1784111725000pay_202607151832120212391PAYIN_202607151832009826USD19.003failFail");
        assertThat(verifier.verify(timestamp, signature, params)).isTrue();
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

    private Map<String, String> payinWebhookParams(String amount) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("merNo", "2607039255");
        params.put("tradeNo", "pay_202607151832120212391");
        params.put("orderNo", "PAYIN_202607151832009826");
        params.put("currency", "USD");
        params.put("amount", amount);
        params.put("paymentMethod", "PAY_PAL");
        params.put("status", "3");
        params.put("code", "fail");
        params.put("message", "Fail");
        params.put("metadata", "myParam=1");
        return params;
    }
}
