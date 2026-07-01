package com.scott.payment.sdk.jwt;

import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

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
class MerchantJwtSignerTest {

    /**
     * 验证 JWT 使用 HS256 并包含后端要求的 claims。
     */
    @Test
    void shouldSignHs256JwtWithRequiredClaims() {
        Date issuedAt = new Date(1_800_000_000_000L);

        String token = new MerchantJwtSigner().sign(
                OpenApiTestSupport.merchantId(),
                OpenApiTestSupport.merchantJwtSecret(),
                OpenApiTestSupport.livemode(),
                "ORDER-1",
                issuedAt,
                180);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(OpenApiTestSupport.merchantJwtSecret().getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getIssuer()).isEqualTo("merchant");
        assertThat(claims.getId()).isEqualTo("ORDER-1");
        assertThat(claims.get("merchantId", String.class)).isEqualTo(OpenApiTestSupport.merchantId());
        assertThat(claims.get("livemode", Boolean.class)).isEqualTo(OpenApiTestSupport.livemode());
        assertThat(claims.get("aud", List.class)).contains("gateway");
    }
}
