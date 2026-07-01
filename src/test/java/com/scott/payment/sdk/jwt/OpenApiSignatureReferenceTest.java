package com.scott.payment.sdk.jwt;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.crypto.RsaKeyUtils;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.common.OpenApiEncryptedRequest;
import com.scott.payment.sdk.model.common.OpenApiPayloadParts;
import com.scott.payment.sdk.model.common.PaymentMethod;
import com.scott.payment.sdk.model.payment.PaymentCreateRequest;
import com.scott.payment.sdk.util.OrderNoGenerator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiSignatureReferenceTest
 * @date : 2026-07-01 15:18
 * @email : scott_x@163.com
 * @description : OpenAPI 签名和请求头参考用例，负责演示商户如何基于 merchant-config.properties 生成 Bearer JWT、Authorization、X-Request-Id、POST 请求体和 GET 请求头。
 *                本测试只构造本地示例数据，不发起真实 HTTP 请求，不创建支付订单，不修改支付、代付、退款、客户、资金、密钥或配置状态。
 *                示例日志用于商户沙盒联调和 Apifox 文档引用，完整 API 私钥和完整 JWT 不得输出，Authorization 日志必须脱敏。
 * @status : create
 */
@Slf4j
class OpenApiSignatureReferenceTest {

    /**
     * POST 示例接口路径。
     *
     * 用途：演示代收创建接口的签名 Header 和加密请求体。
     */
    private static final String POST_PATH = OpenApiConstants.PAYMENT_CREATE_PATH;

    /**
     * GET 示例接口路径。
     *
     * 用途：演示检索代收交易接口无请求体但仍需 Bearer JWT。
     */
    private static final String GET_PATH = "/pay-api/trade/payment/pay_123";

    /**
     * 演示商户如何使用 merchant-config.properties 生成 JWT 和 Authorization。
     *
     * case 目的：展示 Bearer JWT 的生成、解析和脱敏日志输出。
     * 关键输入：2606177036 沙盒商户号、API 私钥、livemode、jti、iat 和 exp。
     * 结果摘要：输出 Authorization 脱敏值和 claims 摘要；不输出完整 JWT 或 API 私钥，不发起 HTTP 请求，不修改资金、密钥或配置。
     */
    @Test
    void shouldGenerateAuthorizationHeaderWithMerchantConfig() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        String jwtId = "payment-" + UUID.randomUUID().toString();
        Date issuedAt = Date.from(Instant.now());

        String token = new MerchantJwtSigner().sign(
                config.getMerchantId(),
                config.getMerchantJwtSecret(),
                config.getLivemode(),
                jwtId,
                issuedAt,
                config.getJwtTtlSeconds());
        String authorization = OpenApiConstants.AUTHORIZATION_PREFIX + token;
        Claims claims = parseClaims(config, token);

        log.info("签名算法示例-Authorization请求头: {}", JsonSupport.toLogJson(logFields(
                OpenApiConstants.HEADER_AUTHORIZATION, OpenApiLogSanitizer.sanitizeHeaders(
                        singletonHeader(OpenApiConstants.HEADER_AUTHORIZATION, authorization))
                        .get(OpenApiConstants.HEADER_AUTHORIZATION))));
        log.info("签名算法示例-JWT Claims摘要: {}", JsonSupport.toLogJson(logFields(
                "issuer", claims.getIssuer(),
                "audience", claims.get("aud", List.class),
                "merchantId", claims.get("merchantId", String.class),
                "livemode", claims.get("livemode", Boolean.class),
                "jti", claims.getId(),
                "iat", claims.getIssuedAt().getTime() / 1000L,
                "exp", claims.getExpiration().getTime() / 1000L)));

        assertThat(authorization).startsWith(OpenApiConstants.AUTHORIZATION_PREFIX);
        assertThat(claims.getIssuer()).isEqualTo("merchant");
        assertThat(claims.get("aud", List.class)).contains("gateway");
        assertThat(claims.get("merchantId", String.class)).isEqualTo(config.getMerchantId());
        assertThat(claims.get("livemode", Boolean.class)).isEqualTo(config.getLivemode());
        assertThat(claims.getId()).isEqualTo(jwtId);
    }

    /**
     * 演示商户如何组装 POST 请求头和加密请求体。
     *
     * case 目的：展示有请求体接口的最终请求结构，包含 Authorization、Content-Type、Accept、User-Agent、X-Request-Id 和 livemode + data。
     * 关键输入：支付创建示例参数、平台请求公钥、2606177036 沙盒商户 API 私钥。
     * 结果摘要：输出请求地址、脱敏请求头、脱敏明文请求、真实密文请求体和密文参数拆分；不调用网关接口，不创建支付订单。
     */
    @Test
    void shouldBuildPostRequestHeadersAndEncryptedBodyForApifox() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        String requestId = UUID.randomUUID().toString();
        String jwtId = "payment-" + requestId;
        PaymentCreateRequest plainRequest = paymentCreateRequest();
        String plainJson = JsonSupport.toJson(plainRequest);
        OpenApiPayloadParts payloadParts = new OpenApiPayloadCrypto().encryptToParts(
                plainJson,
                RsaKeyUtils.readPublicKey(config.getPlatformPublicKey()));
        OpenApiEncryptedRequest encryptedRequest = OpenApiEncryptedRequest.builder()
                .livemode(config.getLivemode())
                .data(payloadParts.toCompactPayload())
                .build();
        Map<String, String> headers = buildHeaders(config, jwtId, requestId, true);

        log.info("签名算法示例-POST请求地址: {}", config.getBaseUri() + POST_PATH);
        log.info("签名算法示例-POST请求头: {}", JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeHeaders(headers)));
        log.info("签名算法示例-POST请求原始明文报文: {}", JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(plainRequest)));
        log.info("签名算法示例-POST请求密文参数: {}", JsonSupport.toLogJson(encryptedRequest));
        log.debug("签名算法示例-POST请求参数拆分: {}", JsonSupport.toLogJson(payloadParts));

        assertThat(headers).containsKeys(
                OpenApiConstants.HEADER_AUTHORIZATION,
                OpenApiConstants.HEADER_CONTENT_TYPE,
                OpenApiConstants.HEADER_ACCEPT,
                OpenApiConstants.HEADER_USER_AGENT,
                OpenApiConstants.HEADER_REQUEST_ID);
        assertThat(encryptedRequest.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(encryptedRequest.getData()).isEqualTo(payloadParts.toCompactPayload());
        assertThat(encryptedRequest.getData().split("\\.", -1)).hasSize(5);
    }

    /**
     * 演示商户如何组装 GET 请求头。
     *
     * case 目的：展示 GET 查询接口没有请求体、不需要 Content-Type，但仍必须携带 Bearer JWT、Accept 和 X-Request-Id。
     * 关键输入：检索代收交易路径、2606177036 沙盒商户 API 私钥和 livemode。
     * 结果摘要：输出请求地址和脱敏请求头；不调用网关接口，不读取或修改交易、资金、密钥或配置状态。
     */
    @Test
    void shouldBuildGetRequestHeadersWithoutBodyForApifox() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        String requestId = UUID.randomUUID().toString();
        String jwtId = "query-" + requestId;
        Map<String, String> headers = buildHeaders(config, jwtId, requestId, false);

        log.info("签名算法示例-GET请求地址: {}", config.getBaseUri() + GET_PATH);
        log.info("签名算法示例-GET请求头: {}", JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeHeaders(headers)));

        assertThat(headers).containsKeys(
                OpenApiConstants.HEADER_AUTHORIZATION,
                OpenApiConstants.HEADER_ACCEPT,
                OpenApiConstants.HEADER_USER_AGENT,
                OpenApiConstants.HEADER_REQUEST_ID);
        assertThat(headers).doesNotContainKey(OpenApiConstants.HEADER_CONTENT_TYPE);
    }

    private Map<String, String> buildHeaders(OpenApiClientConfig config, String jwtId, String requestId, boolean withBody) {
        String token = new MerchantJwtSigner().sign(
                config.getMerchantId(),
                config.getMerchantJwtSecret(),
                config.getLivemode(),
                jwtId,
                Date.from(Instant.now()),
                config.getJwtTtlSeconds());
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(OpenApiConstants.HEADER_AUTHORIZATION, OpenApiConstants.AUTHORIZATION_PREFIX + token);
        headers.put(OpenApiConstants.HEADER_ACCEPT, OpenApiConstants.ACCEPT);
        headers.put(OpenApiConstants.HEADER_USER_AGENT, OpenApiConstants.USER_AGENT);
        headers.put(OpenApiConstants.HEADER_REQUEST_ID, requestId);
        if (withBody) {
            headers.put(OpenApiConstants.HEADER_CONTENT_TYPE, OpenApiConstants.CONTENT_TYPE);
        }
        return headers;
    }

    private Claims parseClaims(OpenApiClientConfig config, String token) {
        return Jwts.parserBuilder()
                .setSigningKey(config.getMerchantJwtSecret().getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private PaymentCreateRequest paymentCreateRequest() {
        Map<String, Object> card = new LinkedHashMap<String, Object>();
        card.put("number", "4242424242424242");
        card.put("expMonth", "06");
        card.put("expYear", "2026");
        card.put("cvc", "123");

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderNo(OrderNoGenerator.generate("PAY"));
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("12.34"));
        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setPaymentMethodData(card);
        return request;
    }

    private Map<String, String> singletonHeader(String key, String value) {
        Map<String, String> header = new HashMap<String, String>(1);
        header.put(key, value);
        return header;
    }

    private Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
