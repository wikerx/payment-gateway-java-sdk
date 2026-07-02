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
 *                本类只用于 SDK 示例和单元测试，不发起真实 HTTP 请求、不访问网关平台、不新增数据库数据、不修改支付、代付、退款或资金状态。
 *                responseData 方法中的 tradeNo、余额、客户等字段都是模拟网关返回，目的是让商户看到 SDK 加密请求和解密响应的完整代码结构。
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
     *
     * 构造时只从测试密钥派生一个响应公钥，用于在内存中模拟网关响应加密；不会读取商户生产配置或访问网关平台。
     */
    public CapturingOpenApiTransport() {
        this.merchantResponsePublicKey = deriveResponsePublicKey(OpenApiTestSupport.clientConfig());
    }

    /**
     * 捕获 SDK 请求并返回模拟响应。
     *
     * 该方法仅在内存中解析请求和生成加密响应，不访问网络、不修改资金或交易状态。
     * 返回的 HTTP 200、code=0 和 data 均为测试模拟结果，不代表网关平台真实交易结果。
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

    /**
     * 设置模拟网关响应中的 livemode。
     *
     * 该方法只影响测试返回值，用于验证 SDK 对响应环境的校验逻辑，不会修改真实商户配置。
     *
     * @param responseLivemode 模拟响应 livemode
     */
    public void setResponseLivemode(Boolean responseLivemode) {
        this.responseLivemode = responseLivemode;
    }

    /**
     * 构造模拟网关响应外壳。
     *
     * 该方法固定返回测试成功 code，并把模拟业务 data 加密为 compact payload。
     * 这里的响应不是网关平台真实响应，只用于验证 SDK 解密链路和商户示例代码可读性。
     *
     * @param data 模拟业务响应数据
     * @return 模拟 OpenAPI 加密响应外壳
     */
    private OpenApiEncryptedResponse envelope(Object data) {
        return OpenApiEncryptedResponse.builder()
                .code(OpenApiConstants.RESPONSE_CODE_SUCCESS)
                .msg("")
                .livemode(responseLivemode)
                .data(encrypted(data))
                .build();
    }

    /**
     * 使用测试公钥加密模拟响应 data。
     *
     * 该方法模拟网关使用商户响应公钥加密 data 的过程，不会访问真实平台密钥服务。
     *
     * @param data 模拟业务响应数据
     * @return compact payload 密文
     */
    private String encrypted(Object data) {
        return crypto.encrypt(JsonSupport.toJson(data), merchantResponsePublicKey);
    }

    /**
     * 根据请求路径构造模拟网关业务响应。
     *
     * 注意：本方法返回的 tradeNo、orderNo、余额、客户资料等字段全部是测试假数据。
     * 这些数据只用于让商户示例用例可以完整演示“SDK 请求加密 -> 模拟网关响应加密 -> SDK 响应解密”流程。
     * 如果需要验证平台真实业务结果，请使用默认 Jdk8HttpTransport 并运行真实调用 case，例如 PayoutTradeTransferTest。
     *
     * @param request SDK 构造的 HTTP 请求
     * @return 模拟业务响应数据
     */
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

    /**
     * 从测试私钥派生模拟网关响应加密所需的公钥。
     *
     * 生产网关会使用平台保存的商户响应公钥加密响应 data；测试传输层没有真实平台服务，因此从测试私钥派生公钥模拟这一过程。
     *
     * @param config 测试客户端配置
     * @return 模拟网关响应加密公钥
     */
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
