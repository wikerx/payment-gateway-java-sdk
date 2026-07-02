package com.scott.payment.sdk.api.webhook.payout.controller;

import com.scott.payment.sdk.api.webhook.payout.PayoutWebhookHandler;
import com.scott.payment.sdk.model.webhook.PayoutWebhookRequest;
import com.scott.payment.sdk.api.webhook.payout.PayoutWebhookVerifier;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutWebhookController
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代付异步通知接收 Controller，负责接收网关以 GET form 参数发送到商户 notifyUrl 的代付结果回调。
 *                本类只做 HTTP 参数接收、签名校验、日志记录和处理器委托，不直接落库、不修改资金、不推进交易状态、不投递 MQ。
 *                商户生产接入时必须在自定义 PayoutWebhookHandler 中基于 tradeNo 或 orderNo 做幂等、终态保护和资金对账。
 * @status : create
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
public class PayoutWebhookController {

    private final PayoutWebhookVerifier verifier;
    private final PayoutWebhookHandler handler;

    public PayoutWebhookController(PayoutWebhookVerifier verifier, PayoutWebhookHandler handler) {
        this.verifier = verifier;
        this.handler = handler;
    }

    /**
     * 接收代付异步通知。
     *
     * 网关当前以 GET + form-url-encoded query 参数发送回调，并在 Header 中携带 `t` 和 `signature`。
     * 签名通过后才委托业务处理器；本方法本身没有事务，不保证幂等，不直接修改资金或交易状态。
     *
     * @param timestamp Header `t`，网关签名时间戳
     * @param signature Header `signature`，网关 SHA-256 hex 签名
     * @param request 代付回调参数
     * @return 验签成功返回 HTTP 200，验签失败返回 HTTP 400
     */
    @GetMapping("/payout")
    public ResponseEntity<String> receivePayout(
            @RequestHeader("t") String timestamp,
            @RequestHeader("signature") String signature,
            PayoutWebhookRequest request) {
        log.info("Receive payout timestamp: {}", timestamp);
        log.info("Receive payout signature: {}", signature);
        log.info("Receive payout request: {}", JsonSupport.toJson(request));

        log.info("代付异步通知-收到回调: {}", JsonSupport.toLogJson(logFields(
                "headers", logHeaders(timestamp, signature),
                "params", OpenApiLogSanitizer.sanitizeObject(request))));

        if (!verifier.verify(timestamp, signature, request)) {
            log.warn("代付异步通知-验签失败: {}", JsonSupport.toLogJson(logFields(
                    "tradeNo", request.getTradeNo(),
                    "orderNo", request.getOrderNo(),
                    "currency", request.getCurrency(),
                    "amount", request.getAmount(),
                    "status", request.getStatus())));
            return ResponseEntity.badRequest().body("invalid signature");
        }

        handler.handle(request);
        log.info("代付异步通知-处理完成: {}", JsonSupport.toLogJson(logFields(
                "tradeNo", request.getTradeNo(),
                "orderNo", request.getOrderNo(),
                "status", request.getStatus(),
                "code", request.getCode())));
        return ResponseEntity.ok("success");
    }

    private Map<String, Object> logHeaders(String timestamp, String signature) {
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("t", timestamp);
        headers.put("signatureLength", signature == null ? 0 : signature.length());
        return headers;
    }

    private Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
