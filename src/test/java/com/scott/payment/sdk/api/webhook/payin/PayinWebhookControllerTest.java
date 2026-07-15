package com.scott.payment.sdk.api.webhook.payin;

import com.scott.payment.sdk.api.webhook.payin.controller.PayinWebhookController;
import com.scott.payment.sdk.model.webhook.PayinWebhookRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinWebhookControllerTest
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代收异步通知 Controller 测试，负责验证 GET 回调参数绑定、Header 签名校验和业务处理器调用边界。
 *                本测试使用 MockMvc，不启动真实端口、不发起外部 HTTP 请求、不修改资金或交易状态。
 * @status : create
 */
class PayinWebhookControllerTest {

    /**
     * 验证代收异步通知验签成功后返回 200 并调用业务处理器。
     */
    @Test
    void receivePayin_withValidSignature_shouldInvokeHandler() throws Exception {
        PayinWebhookVerifier verifier = new PayinWebhookVerifier();
        CountingPayinWebhookHandler handler = new CountingPayinWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayinWebhookController(verifier, handler))
                .build();
        PayinWebhookRequest request = payinWebhookRequest();
        String timestamp = "1782901024000";
        String signature = verifier.sign(timestamp, request);

        mockMvc.perform(get("/api/webhook/payin")
                        .header("t", timestamp)
                        .header("signature", signature)
                        .param("merNo", request.getMerNo())
                        .param("tradeNo", request.getTradeNo())
                        .param("orderNo", request.getOrderNo())
                        .param("currency", request.getCurrency())
                        .param("amount", request.getAmount().toPlainString())
                        .param("paymentMethod", request.getPaymentMethod())
                        .param("status", String.valueOf(request.getStatus()))
                        .param("code", request.getCode())
                        .param("message", request.getMessage())
                        .param("metadata", request.getMetadata()))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        assertThat(handler.count()).isEqualTo(1);
        assertThat(handler.lastRequest().getTradeNo()).isEqualTo("pay_123");
    }

    /**
     * 验证 Controller 使用原始 HTTP 参数验签，避免 amount=19.00 被 BigDecimal 改写后导致签名失败。
     */
    @Test
    void receivePayin_withScaledRawAmount_shouldVerifyAgainstRawParams() throws Exception {
        PayinWebhookVerifier verifier = new PayinWebhookVerifier();
        CountingPayinWebhookHandler handler = new CountingPayinWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayinWebhookController(verifier, handler))
                .build();
        Map<String, String> params = payinWebhookParams("19.00");
        String timestamp = "1784111725000";
        String signature = verifier.sign(timestamp, params);

        mockMvc.perform(get("/api/webhook/payin")
                        .header("t", timestamp)
                        .header("signature", signature)
                        .param("merNo", params.get("merNo"))
                        .param("tradeNo", params.get("tradeNo"))
                        .param("orderNo", params.get("orderNo"))
                        .param("currency", params.get("currency"))
                        .param("amount", params.get("amount"))
                        .param("paymentMethod", params.get("paymentMethod"))
                        .param("status", params.get("status"))
                        .param("code", params.get("code"))
                        .param("message", params.get("message"))
                        .param("metadata", params.get("metadata")))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        assertThat(handler.count()).isEqualTo(1);
        assertThat(handler.lastRequest().getAmount()).isEqualByComparingTo(new BigDecimal("19.00"));
    }

    /**
     * 验证代收异步通知验签失败时返回 400 且不调用业务处理器。
     */
    @Test
    void receivePayin_withInvalidSignature_shouldRejectRequest() throws Exception {
        CountingPayinWebhookHandler handler = new CountingPayinWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayinWebhookController(new PayinWebhookVerifier(), handler))
                .build();

        mockMvc.perform(get("/api/webhook/payin")
                        .header("t", "1782901024000")
                        .header("signature", "bad-signature")
                        .param("tradeNo", "pay_123")
                        .param("orderNo", "ORDER_123")
                        .param("currency", "USD")
                        .param("amount", "12.34")
                        .param("status", "1")
                        .param("code", "success")
                        .param("message", "Paid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("invalid signature"));

        assertThat(handler.count()).isEqualTo(0);
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

    private static final class CountingPayinWebhookHandler implements PayinWebhookHandler {
        private final AtomicInteger count = new AtomicInteger();
        private PayinWebhookRequest lastRequest;

        @Override
        public void handle(PayinWebhookRequest request) {
            this.lastRequest = request;
            this.count.incrementAndGet();
        }

        private int count() {
            return count.get();
        }

        private PayinWebhookRequest lastRequest() {
            return lastRequest;
        }
    }
}
