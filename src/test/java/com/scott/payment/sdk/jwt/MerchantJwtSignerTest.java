package com.scott.payment.sdk.jwt;

import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : MerchantJwtSignerTest
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : 商户 JWT 签名器测试，负责验证 HS256 签名和网关要求的 issuer、audience、merchantId、livemode、jti claims。
 *                本测试只解析本地生成的 JWT，不输出完整 token 或 API 私钥，不发起 HTTP 请求，不修改支付、代付、退款、客户、资金、密钥或配置状态。
 * @status : modify
 */
@Slf4j
class MerchantJwtSignerTest {

    /**
     * 验证 JWT 使用 HS256 并包含后端要求的 claims。
     *
     * case 目的：确认 SDK 使用 merchant-config.properties 中的商户信息生成符合网关验签规则的 JWT。
     * 关键输入：商户号、API 私钥、livemode、jti、签发时间和 180 秒有效期。
     * 结果摘要：只输出 token 脱敏摘要和 claims 摘要，不输出完整 JWT 或 API 私钥。
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
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(OpenApiTestSupport.merchantJwtSecret().getBytes(StandardCharsets.UTF_8))
                .build()
                .parseClaimsJws(token)
                .getBody();
        log.info("JWT签名结果摘要: {}", JsonSupport.toLogJson(logFields(
                "token", OpenApiLogSanitizer.maskToken(token),
                "merchantId", claims.get("merchantId", String.class),
                "livemode", claims.get("livemode", Boolean.class),
                "jti", claims.getId(),
                "issuer", claims.getIssuer(),
                "audience", claims.get("aud", List.class))));

        assertThat(claims.getIssuer()).isEqualTo("merchant");
        assertThat(claims.getId()).isEqualTo(jti);
        assertThat(claims.get("merchantId", String.class)).isEqualTo(OpenApiTestSupport.merchantId());
        assertThat(claims.get("livemode", Boolean.class)).isEqualTo(OpenApiTestSupport.livemode());
        assertThat(claims.get("aud", List.class)).contains("gateway");
    }

    private Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
