package com.wikerx.payment.gateway.sdk.testkit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wikerx.payment.gateway.sdk.config.PaymentGatewayConstants;
import com.wikerx.payment.gateway.sdk.crypto.OpenApiPayloadCrypto;
import com.wikerx.payment.gateway.sdk.crypto.RsaKeyUtils;
import com.wikerx.payment.gateway.sdk.http.HttpTransport;
import com.wikerx.payment.gateway.sdk.http.SdkHttpRequest;
import com.wikerx.payment.gateway.sdk.http.SdkHttpResponse;
import com.wikerx.payment.gateway.sdk.json.JsonSupport;
import lombok.Getter;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 捕获 SDK 请求并模拟网关响应的测试 Transport。
 */
public final class CapturingPaymentGatewayTransport implements HttpTransport {

    private final OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();

    /**
     * 测试服务端用于解密 SDK 请求的平台开户私钥。
     */
    private final PrivateKey platformRequestPrivateKey;

    /**
     * 测试服务端用于加密响应 data 的商户响应公钥。
     */
    private final PublicKey merchantResponsePublicKey;

    /**
     * 最近一次捕获到的 SDK HTTP 请求。
     */
    @Getter
    private SdkHttpRequest lastRequest;

    /**
     * 最近一次捕获到的明文请求体。
     */
    @Getter
    private Map<String, Object> lastPlainBody;

    /**
     * 创建捕获请求的测试传输层。
     */
    public CapturingPaymentGatewayTransport() {
        Properties keyMaterial = PaymentGatewayTestSupport.serviceKeyMaterial();
        this.platformRequestPrivateKey = RsaKeyUtils.readPrivateKey(keyMaterial.getProperty("merchant.platform.private-key"));
        this.merchantResponsePublicKey = RsaKeyUtils.readPublicKey(keyMaterial.getProperty("merchant.response.public-key"));
    }

    /**
     * 捕获 SDK 请求并返回模拟响应。
     */
    @Override
    public SdkHttpResponse execute(SdkHttpRequest request) {
        this.lastRequest = request;
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getBody() != null) {
            this.lastPlainBody = decodeBody(request.getBody());
        }
        Object responseData = responseData(request);
        String body = JsonSupport.toJson(envelope(responseData));
        return SdkHttpResponse.builder()
                .statusCode(200)
                .headers(new HashMap<String, String>())
                .body(body)
                .build();
    }

    private Map<String, Object> decodeBody(String body) {
        Map<String, Object> raw = JsonSupport.fromJson(body, new TypeReference<Map<String, Object>>() {
        });
        Object data = raw.get("data");
        if (data instanceof String && String.valueOf(data).split("\\.").length == 5) {
            String plainJson = crypto.decrypt(String.valueOf(data), platformRequestPrivateKey);
            return JsonSupport.fromJson(plainJson, new TypeReference<Map<String, Object>>() {
            });
        }
        return raw;
    }

    private Map<String, Object> envelope(Object data) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("code", PaymentGatewayConstants.RESPONSE_CODE_SUCCESS);
        response.put("msg", "");
        response.put("livemode", false);
        response.put("data", encrypted(data));
        return response;
    }

    private String encrypted(Object data) {
        return crypto.encrypt(JsonSupport.toJson(data), merchantResponsePublicKey);
    }

    private Object responseData(SdkHttpRequest request) {
        Map<String, Object> data = new HashMap<String, Object>();
        String path = request.getUri().getPath();
        if (path.contains("/fund/accounts")) {
            data.put("merNo", PaymentGatewayTestSupport.merchantId());
            data.put("currency", "USD");
            data.put("balance", "100.00");
            data.put("frozenAmounts", "0.00");
            data.put("withdrawnAmounts", "0.00");
            return data;
        }
        data.put("merNo", PaymentGatewayTestSupport.merchantId());
        data.put("tradeNo", "pay_123");
        data.put("orderNo", lastPlainBody == null ? "ORDER-QUERY" : lastPlainBody.get("orderNo"));
        data.put("currency", "USD");
        data.put("amount", "12.34");
        data.put("status", 1);
        data.put("paymentMethod", "CARD");
        data.put("customerId", "cus_123");
        data.put("firstname", "Ada");
        data.put("lastname", "Lovelace");
        data.put("email", "ada@example.com");
        data.put("country", "US");
        return data;
    }
}
