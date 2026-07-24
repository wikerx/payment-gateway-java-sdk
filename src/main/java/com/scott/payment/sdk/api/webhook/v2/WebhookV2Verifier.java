package com.scott.payment.sdk.api.webhook.v2;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.crypto.RsaKeyUtils;
import com.scott.payment.sdk.exception.OpenApiValidationException;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商户回调 V2 验证器。
 *
 * 本类只负责 Header/JWT 校验、密文 body.data 解密、业务报文解析和 tradeNo 一致性校验。
 * 日志只输出商户号、事件号、交易号、报文长度等摘要信息，不输出完整 JWT、密文 data、解密明文和密钥。
 */
@Slf4j
public class WebhookV2Verifier {

    public static final String HEADER_LIVEMODE = "X-Livemode";
    public static final String HEADER_CALLBACK_VERSION = "X-Callback-Version";
    public static final String HEADER_CALLBACK_EVENT_ID = "X-Callback-Event-Id";
    public static final String CALLBACK_VERSION_V2 = "v2";

    private static final String CALLBACK_ISSUER = "gateway";
    private static final String CALLBACK_AUDIENCE = "merchant";
    private static final String AUTHORIZATION_PREFIX = OpenApiConstants.AUTHORIZATION_PREFIX;

    private final OpenApiClientConfig config;
    private final OpenApiPayloadCrypto payloadCrypto;
    private final PrivateKey merchantResponsePrivateKey;

    public WebhookV2Verifier(OpenApiClientConfig config) {
        this(config, new OpenApiPayloadCrypto());
    }

    public WebhookV2Verifier(OpenApiClientConfig config, OpenApiPayloadCrypto payloadCrypto) {
        this.config = config;
        this.config.validate();
        this.payloadCrypto = payloadCrypto;
        this.merchantResponsePrivateKey = RsaKeyUtils.readPrivateKey(config.getMerchantResponsePrivateKey());
    }

    /**
     * 完成 V2 回调的完整安全处理链路。
     *
     * 只有 Header、JWT、密文和业务 tradeNo 全部校验通过，才会返回业务回调对象。
     */
    public <T> T verifyAndDecrypt(WebhookV2Headers headers,
                                  String requestBody,
                                  String expectedEventType,
                                  Class<T> payloadType) {
        log.info("商户回调V2-开始验签解密: {}", JsonSupport.toLogJson(logFields(
                "livemode", headers == null ? null : headers.getLivemode(),
                "callbackVersion", headers == null ? null : headers.getCallbackVersion(),
                "callbackEventId", headers == null ? null : headers.getCallbackEventId(),
                "expectedEventType", expectedEventType,
                "payloadType", payloadTypeName(payloadType),
                "body", OpenApiLogSanitizer.bodySummary(requestBody))));

        WebhookV2Claims claims = verifyHeadersAndJwt(headers, expectedEventType);
        String plainJson = decryptBody(requestBody);
        log.info("plainJson:{}", plainJson);
        T payload = parsePayload(plainJson, payloadType);
        validateTradeNo(claims, payload);

        log.info("商户回调V2-验签解密完成: {}", JsonSupport.toLogJson(logFields(
                "merchantId", claims.getMerchantId(),
                "livemode", claims.getLivemode(),
                "eventId", claims.getEventId(),
                "eventType", claims.getEventType(),
                "tradeNo", claims.getTradeNo(),
                "payloadType", payloadTypeName(payloadType))));
        return payload;
    }

    /**
     * 校验 V2 回调 Header 和 Authorization JWT。
     *
     * JWT 中的商户号必须与 SDK 配置一致，环境、版本和事件号必须与 Header 绑定一致。
     */
    public WebhookV2Claims verifyHeadersAndJwt(WebhookV2Headers headers, String expectedEventType) {
        validateHeaders(headers);
        Claims claims = parseAndVerifyJwt(headers.getAuthorization());
        WebhookV2Claims verified = toWebhookClaims(claims);

        if (!StringUtils.equals(config.getMerchantId(), verified.getMerchantId())) {
            throw new OpenApiValidationException("callback merchantId is invalid");
        }
        if (!config.getLivemode().equals(verified.getLivemode())) {
            throw new OpenApiValidationException("callback livemode is invalid");
        }
        if (!StringUtils.equals(String.valueOf(config.getLivemode()), headers.getLivemode())) {
            throw new OpenApiValidationException("callback header livemode is inconsistent");
        }
        if (!StringUtils.equals(CALLBACK_VERSION_V2, StringUtils.trim(headers.getCallbackVersion()))) {
            throw new OpenApiValidationException("callback version is invalid");
        }
        if (!StringUtils.equals(headers.getCallbackEventId(), verified.getEventId())
                || !StringUtils.equals(headers.getCallbackEventId(), verified.getJti())) {
            throw new OpenApiValidationException("callback event id is inconsistent");
        }
        if (!StringUtils.equals(expectedEventType, verified.getEventType())) {
            throw new OpenApiValidationException("callback event type is invalid");
        }

        log.info("商户回调V2-JWT与Header校验通过: {}", JsonSupport.toLogJson(logFields(
                "merchantId", verified.getMerchantId(),
                "livemode", verified.getLivemode(),
                "eventId", verified.getEventId(),
                "eventType", verified.getEventType(),
                "tradeNo", verified.getTradeNo())));
        return verified;
    }

    private void validateHeaders(WebhookV2Headers headers) {
        if (headers == null) {
            throw new OpenApiValidationException("callback headers can not be null");
        }
        requireText(headers.getAuthorization(), OpenApiConstants.HEADER_AUTHORIZATION);
        requireText(headers.getLivemode(), HEADER_LIVEMODE);
        requireText(headers.getCallbackVersion(), HEADER_CALLBACK_VERSION);
        requireText(headers.getCallbackEventId(), HEADER_CALLBACK_EVENT_ID);
    }

    private Claims parseAndVerifyJwt(String authorization) {
        String token = extractBearerToken(authorization);
        try {
            Claims claims = Jwts.parserBuilder()
                    .requireIssuer(CALLBACK_ISSUER)
                    .requireAudience(CALLBACK_AUDIENCE)
                    .setSigningKey(Keys.hmacShaKeyFor(config.getMerchantJwtSecret().getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.info("商户回调V2-JWT签名验签通过: {}", JsonSupport.toLogJson(logFields(
                    "issuer", claims.getIssuer(),
                    "audience", claims.getAudience(),
                    "eventId", claims.get("eventId"),
                    "eventType", claims.get("eventType"))));
            return claims;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new OpenApiValidationException("callback JWT is invalid", exception);
        }
    }

    private String extractBearerToken(String authorization) {
        if (!StringUtils.startsWith(authorization, AUTHORIZATION_PREFIX)) {
            throw new OpenApiValidationException("callback Authorization header is invalid");
        }
        String token = StringUtils.trim(StringUtils.substringAfter(authorization, AUTHORIZATION_PREFIX));
        if (StringUtils.isBlank(token)) {
            throw new OpenApiValidationException("callback JWT can not be blank");
        }
        return token;
    }

    private WebhookV2Claims toWebhookClaims(Claims claims) {
        return WebhookV2Claims.builder()
                .merchantId(requiredString(claims, "merchantId"))
                .livemode(requiredBoolean(claims, "livemode"))
                .eventId(requiredString(claims, "eventId"))
                .eventType(requiredString(claims, "eventType"))
                .tradeNo(requiredString(claims, "tradeNo"))
                .jti(requiredString(claims, "jti"))
                .build();
    }

    private String decryptBody(String requestBody) {
        if (StringUtils.isBlank(requestBody)) {
            throw new OpenApiValidationException("callback request body can not be blank");
        }
        try {
            EncryptedWebhookRequest encrypted = JsonSupport.fromJson(requestBody, EncryptedWebhookRequest.class);
            if (StringUtils.isBlank(encrypted.getData())) {
                throw new OpenApiValidationException("callback data can not be blank");
            }
            log.info("商户回调V2-密文体校验通过: {}", JsonSupport.toLogJson(logFields(
                    "body", OpenApiLogSanitizer.bodySummary(requestBody),
                    "data", OpenApiLogSanitizer.encryptedDataSummary(encrypted.getData()))));

            String plainJson = payloadCrypto.decrypt(encrypted.getData(), merchantResponsePrivateKey);
            log.info("商户回调V2-密文体解密完成: {}", JsonSupport.toLogJson(logFields(
                    "plainLength", plainJson == null ? 0 : plainJson.length())));
            return plainJson;
        } catch (OpenApiValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OpenApiValidationException("callback encrypted body is invalid", exception);
        }
    }

    private <T> T parsePayload(String plainJson, Class<T> payloadType) {
        try {
            T payload = JsonSupport.fromJson(plainJson, payloadType);
            log.info("商户回调V2-业务报文解析完成: {}", JsonSupport.toLogJson(logFields(
                    "payloadType", payloadTypeName(payloadType))));
            return payload;
        } catch (RuntimeException exception) {
            throw new OpenApiValidationException("callback payload can not be parsed", exception);
        }
    }

    /**
     * 校验 JWT 中的 tradeNo 与解密后的业务报文 tradeNo 一致。
     *
     * 该校验用于防止 Header/JWT 与 body 被拼接替换后误入商户处理器。
     */
    private <T> void validateTradeNo(WebhookV2Claims claims, T payload) {
        if (payload == null) {
            throw new OpenApiValidationException("callback payload can not be null");
        }
        try {
            Object tradeNo = payload.getClass().getMethod("getTradeNo").invoke(payload);
            if (!StringUtils.equals(claims.getTradeNo(), tradeNo == null ? null : String.valueOf(tradeNo))) {
                throw new OpenApiValidationException("callback tradeNo is inconsistent");
            }
            log.info("商户回调V2-tradeNo一致性校验通过: {}", JsonSupport.toLogJson(logFields(
                    "eventId", claims.getEventId(),
                    "tradeNo", claims.getTradeNo())));
        } catch (OpenApiValidationException exception) {
            throw exception;
        } catch (ReflectiveOperationException exception) {
            throw new OpenApiValidationException("callback payload tradeNo can not be read", exception);
        }
    }

    private String requiredString(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof String && StringUtils.isNotBlank((String) value)) {
            return (String) value;
        }
        throw new OpenApiValidationException("callback JWT " + name + " is missing");
    }

    private Boolean requiredBoolean(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new OpenApiValidationException("callback JWT " + name + " is missing");
    }

    private void requireText(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new OpenApiValidationException(fieldName + " can not be blank");
        }
    }

    public static List<String> maskedAuthorization() {
        return Collections.singletonList("Bearer ***");
    }

    private static String payloadTypeName(Class<?> payloadType) {
        return payloadType == null ? null : payloadType.getSimpleName();
    }

    private static Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
