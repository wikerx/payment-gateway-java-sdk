package com.scott.payment.sdk.api.webhook.payout;

import com.scott.payment.sdk.api.webhook.payout.controller.PayoutWebhookController;
import com.scott.payment.sdk.model.webhook.PayoutWebhookRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutWebhookControllerTest
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代付异步通知 Controller 测试，负责验证 GET 回调参数绑定、Header 签名校验和业务处理器调用边界。
 *                本测试使用 MockMvc，不启动真实端口、不发起外部 HTTP 请求、不修改资金或交易状态。
 * @status : create
 */
class PayoutWebhookControllerTest {

    /**
     * 验证代付异步通知验签成功后返回 200 并调用业务处理器。
     */
    @Test
    void receivePayout_withValidSignature_shouldInvokeHandler() throws Exception {
        PayoutWebhookVerifier verifier = new PayoutWebhookVerifier();
        CountingPayoutWebhookHandler handler = new CountingPayoutWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayoutWebhookController(verifier, handler))
                .build();
        PayoutWebhookRequest request = payoutWebhookRequest();
        String timestamp = "1782901024000";
        String signature = verifier.sign(timestamp, request);

        mockMvc.perform(get("/api/webhook/payout")
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
        assertThat(handler.lastRequest().getTradeNo()).isEqualTo("payout_123");
    }

    /**
     * 验证代付异步通知验签失败时返回 400 且不调用业务处理器。
     */
    @Test
    void receivePayout_withInvalidSignature_shouldRejectRequest() throws Exception {
        CountingPayoutWebhookHandler handler = new CountingPayoutWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayoutWebhookController(new PayoutWebhookVerifier(), handler))
                .build();

        mockMvc.perform(get("/api/webhook/payout")
                        .header("t", "1782901024000")
                        .header("signature", "bad-signature")
                        .param("tradeNo", "payout_123")
                        .param("currency", "USD")
                        .param("amount", "3.11")
                        .param("status", "1")
                        .param("code", "040001003")
                        .param("message", "Failed"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("invalid signature"));

        assertThat(handler.count()).isEqualTo(0);
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

    private static final class CountingPayoutWebhookHandler implements PayoutWebhookHandler {
        private final AtomicInteger count = new AtomicInteger();
        private PayoutWebhookRequest lastRequest;

        @Override
        public void handle(PayoutWebhookRequest request) {
            this.lastRequest = request;
            this.count.incrementAndGet();
        }

        private int count() {
            return count.get();
        }

        private PayoutWebhookRequest lastRequest() {
            return lastRequest;
        }
    }
}
