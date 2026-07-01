package com.scott.payment.sdk.testkit;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.crypto.RsaKeyUtils;
import com.scott.payment.sdk.http.HttpTransport;
import com.scott.payment.sdk.http.SdkHttpRequest;
import com.scott.payment.sdk.http.SdkHttpResponse;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.model.common.OpenApiEncryptedRequest;
import com.scott.payment.sdk.model.common.OpenApiEncryptedResponse;
import lombok.Getter;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CapturingOpenApiTransport
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 内存测试传输层，负责捕获 SDK HTTP 请求、解析加密请求外壳并模拟网关加密响应。
 *                本类只用于单元测试，不发起真实 HTTP 请求、不新增数据库数据、不修改支付、代付、退款或资金状态；响应 data 使用测试公钥加密以验证 SDK 解密链路。
 * @status : create
 */
public final class CapturingOpenApiTransport implements HttpTransport {

    private final OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();

    /**
     * 测试服务端用于加密响应 data 的商户响应公钥。
     *
     * 敏感字段：按密钥材料处理。
     * 是否允许为空：否。
     * 用途：模拟网关响应加密，不参与真实外部渠道调用。
     */
    private final PublicKey merchantResponsePublicKey;

    /**
     * 最近一次捕获到的 SDK HTTP 请求。
     *
     * 是否允许为空：执行请求前允许为空。
     * 用途：断言请求路径、Header 和 body 是否符合最新 OpenAPI 协议。
     */
    @Getter
    private SdkHttpRequest lastRequest;

    /**
     * 最近一次捕获到的密文请求外壳。
     *
     * 是否允许为空：GET 请求或执行请求前允许为空。
     * 用途：断言 POST 请求 livemode 和 data 是否按加密外壳发送。
     */
    @Getter
    private OpenApiEncryptedRequest lastEnvelope;

    /**
     * 模拟网关响应中的 livemode。
     *
     * 是否允许为空：测试中允许设置为空。
     * 用途：验证 SDK 对响应环境一致性的校验。
     */
    private Boolean responseLivemode = OpenApiTestSupport.livemode();

    /**
     * 创建捕获请求的测试传输层。
     */
    public CapturingOpenApiTransport() {
        this.merchantResponsePublicKey = deriveResponsePublicKey(OpenApiTestSupport.clientConfig());
    }

    /**
     * 捕获 SDK 请求并返回模拟响应。
     *
     * 该方法仅在内存中解析请求和生成加密响应，不访问网络、不修改资金或交易状态。
     *
     * @param request SDK 构造的 HTTP 请求
     * @return 模拟网关 HTTP 响应
     */
    @Override
    public SdkHttpResponse execute(SdkHttpRequest request) {
        this.lastRequest = request;
        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getBody() != null) {
            this.lastEnvelope = JsonSupport.fromJson(request.getBody(), OpenApiEncryptedRequest.class);
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

    private OpenApiEncryptedResponse envelope(Object data) {
        return OpenApiEncryptedResponse.builder()
                .code(OpenApiConstants.RESPONSE_CODE_SUCCESS)
                .msg("")
                .livemode(responseLivemode)
                .data(encrypted(data))
                .build();
    }

    private String encrypted(Object data) {
        return crypto.encrypt(JsonSupport.toJson(data), merchantResponsePublicKey);
    }

    private Object responseData(SdkHttpRequest request) {
        Map<String, Object> data = new HashMap<String, Object>();
        String path = request.getUri().getPath();
        if (path.contains("/fund/accounts")) {
            data.put("merNo", OpenApiTestSupport.merchantId());
            data.put("currency", "USD");
            data.put("balance", "100.00");
            data.put("frozenAmounts", "0.00");
            data.put("withdrawnAmounts", "0.00");
            return data;
        }
        data.put("merNo", OpenApiTestSupport.merchantId());
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

    private PublicKey deriveResponsePublicKey(OpenApiClientConfig config) {
        try {
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) RsaKeyUtils.readPrivateKey(config.getMerchantResponsePrivateKey());
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent());
            return KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
        } catch (Exception exception) {
            throw new IllegalStateException("Can not derive merchant response public key", exception);
        }
    }
}
