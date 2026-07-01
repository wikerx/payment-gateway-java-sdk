package com.scott.payment.sdk.jwt;

import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.exception.OpenApiValidationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : MerchantJwtSigner
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : 商户 JWT HS256 签名器，负责按网关 MerchantJwtVerifier 约束生成 Bearer JWT。
 *                本类不加密请求体、不发起 HTTP 请求、不修改资金或交易状态；签名密钥和生成后的 JWT 都属于敏感鉴权材料。
 * @status : modify
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
     * @param livemode   是否生产模式
     * @param jwtId      JWT 防重放标识
     * @param issuedAt   签发时间
     * @param ttlSeconds 有效秒数，不能超过后端 180 秒窗口
     * @return JWT 字符串
     */
    public String sign(String merchantId, String secret, Boolean livemode, String jwtId, Date issuedAt, long ttlSeconds) {
        validate(merchantId, secret, livemode, jwtId, ttlSeconds);
        Date expiresAt = new Date(issuedAt.getTime() + ttlSeconds * 1000L);
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // 同时设置标准 audience 和 aud claim 数组，以兼容后端 MerchantJwtVerifier 的固定 claim 校验。
        return Jwts.builder()
                .setHeaderParam(OpenApiConstants.JWT_HEADER_TYPE, OpenApiConstants.JWT_TYPE)
                .setAudience("gateway")
                .setIssuer("merchant")
                .setId(jwtId)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .claim("aud", Collections.singletonList("gateway"))
                .claim("merchantId", merchantId)
                .claim("livemode", livemode)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 校验 JWT 签名参数，避免生成后端必然拒绝的 token。
     *
     * @param merchantId 商户号
     * @param secret     商户 JWT 签名密钥
     * @param livemode   是否生产模式
     * @param jwtId      JWT 防重放标识
     * @param ttlSeconds JWT 有效秒数
     */
    private void validate(String merchantId, String secret, Boolean livemode, String jwtId, long ttlSeconds) {
        if (StringUtils.isBlank(merchantId)) {
            throw new OpenApiValidationException("merchantId can not be blank");
        }
        if (StringUtils.isBlank(secret)) {
            throw new OpenApiValidationException("merchant jwt secret can not be blank");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < HS256_MIN_SECRET_BYTES) {
            throw new OpenApiValidationException("merchant jwt secret must be at least 256 bits for HS256");
        }
        if (livemode == null) {
            throw new OpenApiValidationException("livemode can not be null");
        }
        if (StringUtils.isBlank(jwtId)) {
            throw new OpenApiValidationException("jwt jti can not be blank");
        }
        if (ttlSeconds <= 0 || ttlSeconds > OpenApiConstants.JWT_TTL_SECONDS) {
            throw new OpenApiValidationException("jwt ttlSeconds must be between 1 and " + OpenApiConstants.JWT_TTL_SECONDS);
        }
    }
}
