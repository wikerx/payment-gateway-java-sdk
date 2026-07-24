package com.scott.payment.sdk.api.webhook.v2;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.crypto.RsaKeyUtils;
import com.scott.payment.sdk.json.JsonSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Date;

/**
 * Test support for locally simulating gateway webhook v2 callbacks.
 */
public final class WebhookV2TestSupport {

    private WebhookV2TestSupport() {
    }

    public static String signCallbackJwt(OpenApiClientConfig config,
                                         String eventId,
                                         String eventType,
                                         String tradeNo) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer("gateway")
                .setAudience("merchant")
                .setId(eventId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(config.getJwtTtlSeconds())))
                .claim("merchantId", config.getMerchantId())
                .claim("livemode", config.getLivemode())
                .claim("eventId", eventId)
                .claim("eventType", eventType)
                .claim("tradeNo", tradeNo)
                .signWith(Keys.hmacShaKeyFor(config.getMerchantJwtSecret().getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    public static String encryptedBody(OpenApiClientConfig config, Object callbackPayload) throws Exception {
        String encryptedData = new OpenApiPayloadCrypto().encrypt(JsonSupport.toJson(callbackPayload),
                merchantResponsePublicKey(config));
        return JsonSupport.toJson(encryptedData(encryptedData));
    }

    private static EncryptedWebhookRequest encryptedData(String data) {
        EncryptedWebhookRequest request = new EncryptedWebhookRequest();
        request.setData(data);
        return request;
    }

    public static PublicKey merchantResponsePublicKey(OpenApiClientConfig config) throws Exception {
        PrivateKey privateKey = RsaKeyUtils.readPrivateKey(config.getMerchantResponsePrivateKey());
        RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                rsaPrivateKey.getModulus(),
                rsaPrivateKey.getPublicExponent());
        return KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
    }
}
