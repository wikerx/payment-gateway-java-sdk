package com.scott.payment.sdk.api.webhook.payin;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.api.webhook.payin.controller.PayinWebhookV2Controller;
import com.scott.payment.sdk.api.webhook.v2.WebhookV2Claims;
import com.scott.payment.sdk.api.webhook.v2.WebhookV2TestSupport;
import com.scott.payment.sdk.api.webhook.v2.WebhookV2Verifier;
import com.scott.payment.sdk.model.webhook.PayinWebhookRequest;
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
 * Pay-in webhook v2 controller tests.
 */
class PayinWebhookV2ControllerTest {

    @Test
    void receivePayinWebhook_withValidV2Callback_shouldInvokeHandler() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        CountingPayinWebhookHandler handler = new CountingPayinWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayinWebhookV2Controller(new WebhookV2Verifier(config), handler))
                .build();
        PayinWebhookRequest request = payinWebhookRequest();
        String eventId = "evt-payin-001";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, "PAYIN_CALLBACK", request.getTradeNo());
        String body = WebhookV2TestSupport.encryptedBody(config, request);

        mockMvc.perform(post("/api/v2/webhook/payin")
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
        assertThat(handler.lastRequest().getAmount()).isEqualByComparingTo(new BigDecimal("12.34"));
    }

    @Test
    void receivePayinWebhook_withWrongEventType_shouldRejectAndNotInvokeHandler() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        CountingPayinWebhookHandler handler = new CountingPayinWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayinWebhookV2Controller(new WebhookV2Verifier(config), handler))
                .build();
        PayinWebhookRequest request = payinWebhookRequest();
        String eventId = "evt-payin-002";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, "PAYOUT_CALLBACK", request.getTradeNo());
        String body = WebhookV2TestSupport.encryptedBody(config, request);

        mockMvc.perform(post("/api/v2/webhook/payin")
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

    @Test
    void receivePayinWebhook_withV2AwareHandler_shouldPassVerifiedClaims() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        V2AwarePayinWebhookHandler handler = new V2AwarePayinWebhookHandler();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PayinWebhookV2Controller(new WebhookV2Verifier(config), handler))
                .build();
        PayinWebhookRequest request = payinWebhookRequest();
        String eventId = "evt-payin-claims-001";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, "PAYIN_CALLBACK", request.getTradeNo());
        String body = WebhookV2TestSupport.encryptedBody(config, request);

        mockMvc.perform(post("/api/v2/webhook/payin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .header("X-Livemode", String.valueOf(config.getLivemode()))
                        .header("X-Callback-Version", "v2")
                        .header("X-Callback-Event-Id", eventId)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        assertThat(handler.count()).isEqualTo(1);
        assertThat(handler.lastClaims().getEventId()).isEqualTo(eventId);
        assertThat(handler.lastClaims().getMerchantId()).isEqualTo(config.getMerchantId());
    }

    private PayinWebhookRequest payinWebhookRequest() {
        PayinWebhookRequest request = new PayinWebhookRequest();
        request.setMerNo(OpenApiTestSupport.merchantId());
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

    private static final class V2AwarePayinWebhookHandler implements PayinWebhookHandler {
        private final AtomicInteger count = new AtomicInteger();
        private WebhookV2Claims lastClaims;

        @Override
        public void handle(PayinWebhookRequest request) {
            throw new AssertionError("V2 controller should pass verified claims to V2-aware handlers");
        }

        @Override
        public void handle(PayinWebhookRequest request, WebhookV2Claims claims) {
            this.lastClaims = claims;
            this.count.incrementAndGet();
        }

        private int count() {
            return count.get();
        }

        private WebhookV2Claims lastClaims() {
            return lastClaims;
        }
    }
}
