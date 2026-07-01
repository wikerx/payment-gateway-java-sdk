package com.scott.payment.sdk.jwt;

import com.scott.payment.sdk.testkit.PaymentGatewayTestSupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantJwtSignerTest {

    /**
     * 验证 JWT 使用 HS256 并包含后端要求的 claims。
     */
    @Test
    void shouldSignHs256JwtWithRequiredClaims() {
        Date issuedAt = new Date(1_800_000_000_000L);

        String token = new MerchantJwtSigner().sign(
                PaymentGatewayTestSupport.merchantId(),
                PaymentGatewayTestSupport.merchantJwtSecret(),
                PaymentGatewayTestSupport.livemode(),
                "ORDER-1",
                issuedAt,
                180);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(PaymentGatewayTestSupport.merchantJwtSecret().getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getIssuer()).isEqualTo("merchant");
        assertThat(claims.getId()).isEqualTo("ORDER-1");
        assertThat(claims.get("merchantId", String.class)).isEqualTo(PaymentGatewayTestSupport.merchantId());
        assertThat(claims.get("livemode", Boolean.class)).isEqualTo(PaymentGatewayTestSupport.livemode());
        assertThat(claims.get("aud", List.class)).contains("gateway");
    }
}
