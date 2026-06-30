package com.wikerx.payment.gateway.sdk.jwt;

import com.wikerx.payment.gateway.sdk.config.PaymentGatewayConstants;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayValidationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;

/**
 * 商户 JWT HS256 签名器，对齐后端 MerchantJwtVerifier 的固定 claim 约束。
 */
public class MerchantJwtSigner {

    /**
     * HS256 最小密钥字节数。
     */
    private static final int HS256_MIN_SECRET_BYTES = 32;

    /**
     * 签发商户 OpenAPI JWT。
     *
     * @param merchantId 商户号
     * @param secret     商户 JWT 签名密钥
     * @param jwtId      JWT 防重放标识
     * @param issuedAt   签发时间
     * @param ttlSeconds 有效秒数，不能超过后端 180 秒窗口
     * @return JWT 字符串
     */
    public String sign(String merchantId, String secret, String jwtId, Date issuedAt, long ttlSeconds) {
        validate(merchantId, secret, jwtId, ttlSeconds);
        Date expiresAt = new Date(issuedAt.getTime() + ttlSeconds * 1000L);
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // 同时设置标准 audience 和 aud claim 数组，以兼容后端 MerchantJwtVerifier 的固定 claim 校验。
        return Jwts.builder()
                .setHeaderParam(PaymentGatewayConstants.JWT_HEADER_TYPE, PaymentGatewayConstants.JWT_TYPE)
                .setAudience("gateway")
                .setIssuer("merchant")
                .setId(jwtId)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .claim("aud", Collections.singletonList("gateway"))
                .claim("merchantId", merchantId)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 校验 JWT 签名参数，避免生成后端必然拒绝的 token。
     *
     * @param merchantId 商户号
     * @param secret     商户 JWT 签名密钥
     * @param jwtId      JWT 防重放标识
     * @param ttlSeconds JWT 有效秒数
     */
    private void validate(String merchantId, String secret, String jwtId, long ttlSeconds) {
        if (StringUtils.isBlank(merchantId)) {
            throw new PaymentGatewayValidationException("merchantId can not be blank");
        }
        if (StringUtils.isBlank(secret)) {
            throw new PaymentGatewayValidationException("merchant jwt secret can not be blank");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < HS256_MIN_SECRET_BYTES) {
            throw new PaymentGatewayValidationException("merchant jwt secret must be at least 256 bits for HS256");
        }
        if (StringUtils.isBlank(jwtId)) {
            throw new PaymentGatewayValidationException("jwt jti can not be blank");
        }
        if (ttlSeconds <= 0 || ttlSeconds > PaymentGatewayConstants.JWT_TTL_SECONDS) {
            throw new PaymentGatewayValidationException("jwt ttlSeconds must be between 1 and " + PaymentGatewayConstants.JWT_TTL_SECONDS);
        }
    }
}
