package com.scott.payment.sdk.testkit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scott.payment.sdk.PaymentGatewayClientConfig;
import com.scott.payment.sdk.config.PaymentGatewayConstants;
import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.crypto.RsaKeyUtils;
import com.scott.payment.sdk.http.HttpTransport;
import com.scott.payment.sdk.http.SdkHttpRequest;
import com.scott.payment.sdk.http.SdkHttpResponse;
import com.scott.payment.sdk.json.JsonSupport;
import lombok.Getter;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * 捕获 SDK 请求并模拟网关响应的测试 Transport。
 */
public final class CapturingPaymentGatewayTransport implements HttpTransport {

    private final OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();

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
     * 最近一次捕获到的密文请求外壳。
     */
    @Getter
    private Map<String, Object> lastEnvelope;

    /**
     * 模拟网关响应中的 livemode。
     */
    private Boolean responseLivemode = PaymentGatewayTestSupport.livemode();

    /**
     * 创建捕获请求的测试传输层。
     */
    public CapturingPaymentGatewayTransport() {
        this.merchantResponsePublicKey = deriveResponsePublicKey(PaymentGatewayTestSupport.clientConfig());
    }

    /**
     * 捕获 SDK 请求并返回模拟响应。
     */
    @Override
    public SdkHttpResponse execute(SdkHttpRequest request) {
        this.lastRequest = request;
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getBody() != null) {
            this.lastEnvelope = JsonSupport.fromJson(request.getBody(), new TypeReference<Map<String, Object>>() {
            });
        }
        Object responseData = responseData(request);
        String body = JsonSupport.toJson(envelope(responseData));
        return SdkHttpResponse.builder()
                .statusCode(200)
                .headers(new HashMap<String, String>())
                .body(body)
                .build();
    }

    public void setResponseLivemode(Boolean responseLivemode) {
        this.responseLivemode = responseLivemode;
    }

    private Map<String, Object> envelope(Object data) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("code", PaymentGatewayConstants.RESPONSE_CODE_SUCCESS);
        response.put("msg", "");
        response.put("livemode", responseLivemode);
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
        data.put("orderNo", "ORDER-QUERY");
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

    private PublicKey deriveResponsePublicKey(PaymentGatewayClientConfig config) {
        try {
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) RsaKeyUtils.readPrivateKey(config.getMerchantResponsePrivateKey());
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
            return KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
        } catch (Exception exception) {
            throw new IllegalStateException("Can not derive merchant response public key", exception);
        }
    }
}
