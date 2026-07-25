package com.scott.payment.sdk.api.webhook.payout.controller;

import com.scott.payment.sdk.api.webhook.payout.PayoutWebhookHandler;
import com.scott.payment.sdk.api.webhook.v2.WebhookV2Headers;
import com.scott.payment.sdk.api.webhook.v2.WebhookV2VerificationResult;
import com.scott.payment.sdk.api.webhook.v2.WebhookV2Verifier;
import com.scott.payment.sdk.exception.OpenApiValidationException;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.webhook.PayoutWebhookRequest;
import com.scott.payment.sdk.util.RequestHeaderParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代付回调 V2 接收 Controller。
 *
 * 本类负责接收网关推送的加密回调，将验签、解密和 tradeNo 一致性校验委托给 WebhookV2Verifier，
 * 校验通过后再交给商户自定义 PayoutWebhookHandler 处理。
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/webhook")
public class PayoutWebhookV2Controller {

    private static final String SUCCESS_RESPONSE = "success";
    private static final String INVALID_RESPONSE = "invalid callback";
    private static final String EVENT_TYPE = "PAYOUT_CALLBACK";

    private final WebhookV2Verifier verifier;
    private final PayoutWebhookHandler handler;

    public PayoutWebhookV2Controller(WebhookV2Verifier verifier, PayoutWebhookHandler handler) {
        this.verifier = verifier;
        this.handler = handler;
    }

    /**
     * 接收代付 V2 加密回调。
     *
     * 只有验签、解密和业务处理全部成功才返回 success；非法回调返回非 success，便于网关继续按重试策略补偿。
     */
    @PostMapping(
            value = "/payout",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> receivePayoutWebhook(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-Livemode") String livemode,
            @RequestHeader("X-Callback-Version") String callbackVersion,
            @RequestHeader("X-Callback-Event-Id") String callbackEventId,
            @RequestBody String requestBody,
            HttpServletRequest servletRequest) {
        Map<String, List<String>> requestHeaders = RequestHeaderParams.getRequestHeaders(servletRequest);
        log.info("代付回调V2-收到网关回调: {}", JsonSupport.toLogJson(logFields(
                "method", servletRequest.getMethod(),
                "uri", servletRequest.getRequestURI(),
                "livemode", livemode,
                "callbackVersion", callbackVersion,
                "callbackEventId", callbackEventId,
                "headers", requestHeaders,
                "body", OpenApiLogSanitizer.bodySummary(requestBody))));
        WebhookV2VerificationResult<PayoutWebhookRequest> verified = verifier.verifyAndDecryptWithClaims(WebhookV2Headers.builder()
                        .authorization(authorization)
                        .livemode(livemode)
                        .callbackVersion(callbackVersion)
                        .callbackEventId(callbackEventId)
                        .build(),
                requestBody,
                EVENT_TYPE,
                PayoutWebhookRequest.class);
        PayoutWebhookRequest request = verified.getPayload();

        handler.handle(request, verified.getClaims());
        log.info("代付回调V2-处理完成: {}", JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(logFields(
                "callbackEventId", callbackEventId,
                "tradeNo", request.getTradeNo(),
                "orderNo", request.getOrderNo(),
                "status", request.getStatus(),
                "code", request.getCode()))));
        return ResponseEntity.ok(SUCCESS_RESPONSE);
    }

    @ExceptionHandler(OpenApiValidationException.class)
    public ResponseEntity<String> handleValidationException(OpenApiValidationException exception) {
        log.warn("代付回调V2-拒绝非法回调: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(INVALID_RESPONSE);
    }

    private Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
