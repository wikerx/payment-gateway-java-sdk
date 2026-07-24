package com.scott.payment.sdk.api.webhook.payout;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.api.webhook.payout.controller.PayoutWebhookV2Controller;
import com.scott.payment.sdk.api.webhook.v2.WebhookV2TestSupport;
import com.scott.payment.sdk.api.webhook.v2.WebhookV2Verifier;
import com.scott.payment.sdk.model.webhook.PayoutWebhookRequest;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Payout webhook v2 controller tests.
 */
class PayoutWebhookV2ControllerTest {

    @Test
    void receivePayoutWebhook_withValidV2Callback_shouldInvokeHandler() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        CountingPayoutWebhookHandler handler = new CountingPayoutWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayoutWebhookV2Controller(new WebhookV2Verifier(config), handler))
                .build();
        PayoutWebhookRequest request = payoutWebhookRequest();
        String eventId = "evt-payout-001";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, "PAYOUT_CALLBACK", request.getTradeNo());
        String body = WebhookV2TestSupport.encryptedBody(config, request);

        mockMvc.perform(post("/api/v2/webhook/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .header("X-Livemode", String.valueOf(config.getLivemode()))
                        .header("X-Callback-Version", "v2")
                        .header("X-Callback-Event-Id", eventId)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        assertThat(handler.count()).isEqualTo(1);
        assertThat(handler.lastRequest().getTradeNo()).isEqualTo(request.getTradeNo());
        assertThat(handler.lastRequest().getAmount()).isEqualByComparingTo(new BigDecimal("19.00"));
    }

    @Test
    void receivePayoutWebhook_withWrongEventType_shouldRejectAndNotInvokeHandler() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        CountingPayoutWebhookHandler handler = new CountingPayoutWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayoutWebhookV2Controller(new WebhookV2Verifier(config), handler))
                .build();
        PayoutWebhookRequest request = payoutWebhookRequest();
        String eventId = "evt-payout-002";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, "PAYIN_CALLBACK", request.getTradeNo());
        String body = WebhookV2TestSupport.encryptedBody(config, request);

        mockMvc.perform(post("/api/v2/webhook/payout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .header("X-Livemode", String.valueOf(config.getLivemode()))
                .header("X-Callback-Version", "v2")
                .header("X-Callback-Event-Id", eventId)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("invalid callback"));

        assertThat(handler.count()).isEqualTo(0);
    }

    private PayoutWebhookRequest payoutWebhookRequest() {
        PayoutWebhookRequest request = new PayoutWebhookRequest();
        request.setMerNo(OpenApiTestSupport.merchantId());
        request.setTradeNo("payout_123");
        request.setOrderNo("PAYOUT_ORDER_123");
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("19.00"));
        request.setPaymentMethod("ACH_DEBIT");
        request.setCompletionDate("2026-07-23 15:30:00");
        request.setStatus(1);
        request.setCode("success");
        request.setMessage("Paid");
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
