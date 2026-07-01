package com.scott.payment.sdk.jwt;

import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : MerchantJwtSignerTest
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : 商户 JWT 签名器测试，负责验证 HS256 签名和网关要求的 issuer、audience、merchantId、livemode、jti claims。
 *                本测试只解析本地生成的 JWT，不输出完整 token、不发起 HTTP 请求、不修改资金或交易状态。
 * @status : modify
 */
@Slf4j
class MerchantJwtSignerTest {

    /**
     * 验证 JWT 使用 HS256 并包含后端要求的 claims。
     */
    @Test
    void shouldSignHs256JwtWithRequiredClaims() {
        Date issuedAt = new Date(1_800_000_000_000L);
        String jti = "ORDER-" + UUID.randomUUID().toString();

        String token = new MerchantJwtSigner().sign(
                OpenApiTestSupport.merchantId(),
                OpenApiTestSupport.merchantJwtSecret(),
                OpenApiTestSupport.livemode(),
                jti,
                issuedAt,
                180);
        log.info("token: {}", token);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(OpenApiTestSupport.merchantJwtSecret().getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
        log.info("claims: {}", JsonSupport.toJson(claims));

        assertThat(claims.getIssuer()).isEqualTo("merchant");
        assertThat(claims.getId()).isEqualTo(jti);
        assertThat(claims.get("merchantId", String.class)).isEqualTo(OpenApiTestSupport.merchantId());
        assertThat(claims.get("livemode", Boolean.class)).isEqualTo(OpenApiTestSupport.livemode());
        assertThat(claims.get("aud", List.class)).contains("gateway");
    }
}
