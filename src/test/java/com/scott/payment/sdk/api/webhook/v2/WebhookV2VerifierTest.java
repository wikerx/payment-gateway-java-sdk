package com.scott.payment.sdk.api.webhook.v2;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.exception.OpenApiValidationException;
import com.scott.payment.sdk.model.webhook.PayinWebhookRequest;
import com.scott.payment.sdk.model.webhook.PayoutWebhookRequest;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Webhook v2 verification tests.
 */
class WebhookV2VerifierTest {

    private static final String EVENT_TYPE = "PAYIN_CALLBACK";

    @Test
    void verifyAndDecrypt_withValidCallback_shouldReturnPayload() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-001";

        PayinWebhookRequest result = new WebhookV2Verifier(config).verifyAndDecrypt(
                headers(config, eventId, WebhookV2TestSupport.signCallbackJwt(config, eventId, EVENT_TYPE,
                        request.getTradeNo())),
                WebhookV2TestSupport.encryptedBody(config, request),
                EVENT_TYPE,
                PayinWebhookRequest.class);

        assertThat(result.getTradeNo()).isEqualTo("pay_123");
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("12.34"));
    }

    @Test
    void verifyAndDecryptWithClaims_withValidCallback_shouldReturnClaimsAndPayload() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-claims-001";

        WebhookV2VerificationResult<PayinWebhookRequest> result = new WebhookV2Verifier(config)
                .verifyAndDecryptWithClaims(
                        headers(config, eventId, WebhookV2TestSupport.signCallbackJwt(config, eventId, EVENT_TYPE,
                                request.getTradeNo())),
                        WebhookV2TestSupport.encryptedBody(config, request),
                        EVENT_TYPE,
                        PayinWebhookRequest.class);

        assertThat(result.getPayload().getTradeNo()).isEqualTo("pay_123");
        assertThat(result.getClaims().getEventId()).isEqualTo(eventId);
        assertThat(result.getClaims().getMerchantId()).isEqualTo(config.getMerchantId());
    }

    @Test
    void verifyAndDecrypt_withGatewayNumericPayinTimeFields_shouldParseAsText() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        String eventId = "evt-verifier-payin-time-001";
        String plainJson = "{"
                + "\"merNo\":\"" + OpenApiTestSupport.merchantId() + "\","
                + "\"tradeNo\":\"pay_numeric_time\","
                + "\"orderNo\":\"ORDER_NUMERIC_TIME\","
                + "\"currency\":\"USD\","
                + "\"amount\":12.34,"
                + "\"paymentMethod\":\"CASHAPP\","
                + "\"tradeDate\":1728641594235,"
                + "\"status\":1,"
                + "\"code\":\"success\","
                + "\"message\":\"Paid\","
                + "\"expireTime\":1728645148000,"
                + "\"paymentMethodTypes\":[\"CASHAPP\"]"
                + "}";

        PayinWebhookRequest result = new WebhookV2Verifier(config).verifyAndDecrypt(
                headers(config, eventId, WebhookV2TestSupport.signCallbackJwt(config, eventId, EVENT_TYPE,
                        "pay_numeric_time")),
                WebhookV2TestSupport.encryptedBodyFromPlainJson(config, plainJson),
                EVENT_TYPE,
                PayinWebhookRequest.class);

        assertThat(result.getTradeNo()).isEqualTo("pay_numeric_time");
        assertThat(result.getTradeDate()).isEqualTo("1728641594235");
        assertThat(result.getExpireTime()).isEqualTo("1728645148000");
    }

    @Test
    void verifyAndDecrypt_withGatewayNumericPayoutTimeFields_shouldParseAsText() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        String eventType = "PAYOUT_CALLBACK";
        String eventId = "evt-verifier-payout-time-001";
        String plainJson = "{"
                + "\"merNo\":\"" + OpenApiTestSupport.merchantId() + "\","
                + "\"tradeNo\":\"payout_numeric_time\","
                + "\"orderNo\":\"PAYOUT_ORDER_NUMERIC_TIME\","
                + "\"currency\":\"USD\","
                + "\"amount\":19.00,"
                + "\"paymentMethod\":\"ACH_DEBIT\","
                + "\"completionDate\":1728641594235,"
                + "\"status\":1,"
                + "\"code\":\"success\","
                + "\"message\":\"Paid\","
                + "\"metadata\":\"metadata\""
                + "}";

        PayoutWebhookRequest result = new WebhookV2Verifier(config).verifyAndDecrypt(
                headers(config, eventId, WebhookV2TestSupport.signCallbackJwt(config, eventId, eventType,
                        "payout_numeric_time")),
                WebhookV2TestSupport.encryptedBodyFromPlainJson(config, plainJson),
                eventType,
                PayoutWebhookRequest.class);

        assertThat(result.getTradeNo()).isEqualTo("payout_numeric_time");
        assertThat(result.getCompletionDate()).isEqualTo("1728641594235");
    }

    @Test
    void verifyAndDecrypt_withTamperedJwt_shouldReject() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-002";

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new WebhookV2Verifier(config).verifyAndDecrypt(
                        headers(config, eventId, "invalid.jwt.value"),
                        WebhookV2TestSupport.encryptedBody(config, request),
                        EVENT_TYPE,
                        PayinWebhookRequest.class);
            }
        }).isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("callback JWT is invalid");
    }

    @Test
    void verifyAndDecrypt_withWrongMerchantClaim_shouldReject() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        OpenApiClientConfig otherMerchantConfig = copyConfig(config, "merchant-other", config.getLivemode());
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-003";
        String jwt = WebhookV2TestSupport.signCallbackJwt(otherMerchantConfig, eventId, EVENT_TYPE,
                request.getTradeNo());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new WebhookV2Verifier(config).verifyAndDecrypt(
                        headers(config, eventId, jwt),
                        WebhookV2TestSupport.encryptedBody(config, request),
                        EVENT_TYPE,
                        PayinWebhookRequest.class);
            }
        }).isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("callback merchantId is invalid");
    }

    @Test
    void verifyAndDecrypt_withWrongLivemodeClaim_shouldReject() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        OpenApiClientConfig otherModeConfig = copyConfig(config, config.getMerchantId(), !config.getLivemode());
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-004";
        String jwt = WebhookV2TestSupport.signCallbackJwt(otherModeConfig, eventId, EVENT_TYPE,
                request.getTradeNo());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new WebhookV2Verifier(config).verifyAndDecrypt(
                        headers(config, eventId, jwt),
                        WebhookV2TestSupport.encryptedBody(config, request),
                        EVENT_TYPE,
                        PayinWebhookRequest.class);
            }
        }).isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("callback livemode is invalid");
    }

    @Test
    void verifyAndDecrypt_withWrongCallbackVersion_shouldReject() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-005";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, EVENT_TYPE, request.getTradeNo());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new WebhookV2Verifier(config).verifyAndDecrypt(
                        WebhookV2Headers.builder()
                                .authorization("Bearer " + jwt)
                                .livemode(String.valueOf(config.getLivemode()))
                                .callbackVersion("v1")
                                .callbackEventId(eventId)
                                .build(),
                        WebhookV2TestSupport.encryptedBody(config, request),
                        EVENT_TYPE,
                        PayinWebhookRequest.class);
            }
        }).isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("callback version is invalid");
    }

    @Test
    void verifyAndDecrypt_withHeaderEventIdMismatch_shouldReject() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-006";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, EVENT_TYPE, request.getTradeNo());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new WebhookV2Verifier(config).verifyAndDecrypt(
                        headers(config, "evt-other", jwt),
                        WebhookV2TestSupport.encryptedBody(config, request),
                        EVENT_TYPE,
                        PayinWebhookRequest.class);
            }
        }).isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("callback event id is inconsistent");
    }

    @Test
    void verifyAndDecrypt_withTradeNoMismatch_shouldReject() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-007";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, EVENT_TYPE, "pay_other");

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new WebhookV2Verifier(config).verifyAndDecrypt(
                        headers(config, eventId, jwt),
                        WebhookV2TestSupport.encryptedBody(config, request),
                        EVENT_TYPE,
                        PayinWebhookRequest.class);
            }
        }).isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("callback tradeNo is inconsistent");
    }

    @Test
    void verifyAndDecrypt_withBlankData_shouldReject() {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-008";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, EVENT_TYPE, request.getTradeNo());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                new WebhookV2Verifier(config).verifyAndDecrypt(
                        headers(config, eventId, jwt),
                        "{\"data\":\"\"}",
                        EVENT_TYPE,
                        PayinWebhookRequest.class);
            }
        }).isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("callback data can not be blank");
    }

    @Test
    void verifyAndDecrypt_withInvalidEncryptedBody_shouldReject() {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        PayinWebhookRequest request = payinWebhookRequest("pay_123");
        String eventId = "evt-verifier-009";
        String jwt = WebhookV2TestSupport.signCallbackJwt(config, eventId, EVENT_TYPE, request.getTradeNo());

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                new WebhookV2Verifier(config).verifyAndDecrypt(
                        headers(config, eventId, jwt),
                        "{\"data\":\"invalid\"}",
                        EVENT_TYPE,
                        PayinWebhookRequest.class);
            }
        }).isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("callback encrypted body is invalid");
    }

    private WebhookV2Headers headers(OpenApiClientConfig config, String eventId, String jwt) {
        return WebhookV2Headers.builder()
                .authorization("Bearer " + jwt)
                .livemode(String.valueOf(config.getLivemode()))
                .callbackVersion("v2")
                .callbackEventId(eventId)
                .build();
    }

    private PayinWebhookRequest payinWebhookRequest(String tradeNo) {
        PayinWebhookRequest request = new PayinWebhookRequest();
        request.setMerNo(OpenApiTestSupport.merchantId());
        request.setTradeNo(tradeNo);
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

    private OpenApiClientConfig copyConfig(OpenApiClientConfig source, String merchantId, Boolean livemode) {
        return OpenApiClientConfig.builder()
                .baseUrl(source.getBaseUrl())
                .merchantId(merchantId)
                .merchantJwtSecret(source.getMerchantJwtSecret())
                .livemode(livemode)
                .platformPublicKey(source.getPlatformPublicKey())
                .merchantResponsePrivateKey(source.getMerchantResponsePrivateKey())
                .jwtTtlSeconds(source.getJwtTtlSeconds())
                .connectTimeoutMs(source.getConnectTimeoutMs())
                .readTimeoutMs(source.getReadTimeoutMs())
                .defaultVersion(source.getDefaultVersion())
                .rawHttpLogEnabled(source.getRawHttpLogEnabled())
                .clock(source.getClock())
                .build();
    }
}
