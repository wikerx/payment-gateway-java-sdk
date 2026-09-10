package com.scott.payment.sdk.config;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.crypto.RsaKeyUtils;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : MerchantConfigLoaderTest
 * @date : 2026-06-30 10:28
 * @email : scott-***@163.com
 * @description : 商户配置加载测试，负责验证 SDK 只读取 merchant-config.properties，并能解析沙盒商户的文本密钥和 PEM 文件。
 *                本测试不将 API 私钥或商户响应私钥写入日志，不新增数据库数据，也不发起真实网关请求。
 * @status : modify
 */
class MerchantConfigLoaderTest {

    /**
     * 商户请求平台加密公钥文本，X.509 DER Base64 格式。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：验证 SDK 支持商户直接通过配置文本接入平台请求公钥。
     */
    private static final String PLATFORM_REQUEST_PUBLIC_KEY_TEXT = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA27x6pgsZdnYy+aIvPQV7GKom+QGIopTuW4kMF6aLy4aSVmgnYDhM702Xu5T+ifXCvA9nuGsUzgcZnkbkH1gdMJRCoONTJHaLhpE8GCe61EE1xgpUNlIB9zWvPek1JGA2IPaJPbwM1MKNuHOlabMpycuLScD8BQXLuETTwTwIliYRzJ94OWSLL79yvPpKY1HK0//p5s6HGgLOZE/ske7FiCgQ573oR4prvi3p9iujjEDGLObpPowPrAqEkBnwYUg6EgkVi9pWck56dkmAU9bmiW20u3+DOTdpZ+BPGKVJH3DXpfCIal558LFOWbo86zHvkNbIekZZ3qs+d55qq6VIDQIDAQAB";

    /**
     * 商户解密平台响应私钥文本，PKCS#8 DER Base64 格式。
     *
     * 敏感字段：是。
     * 是否允许为空：否。
     * 用途：验证 SDK 支持商户直接通过配置文本接入响应解密私钥；测试不得输出该字段。
     */
    private static final String MERCHANT_RESPONSE_PRIVATE_KEY_TEXT = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCoidDTHomZADAX+F2vy6clgPLsQaLO5xIR41TrvLnU8FRa0Cr8RGBomqxwi9wZ7ihLOpoM6PLQjHaA+LwrHxQIsZae+kprw5n+MJoK7iRHD04DQ0jdunytT0knV8tw2aVXCd5SMG5jof7I5QPJNnDZ1iDzn2ecs1Nw42ER+urKtU5jEAUWHc5QkAKwXeTpsFm7Faydgln1xnvFrV9AGFt6gnSGUEr+CeyxqKZZMUa3AaIlVG3PO22P+W9OCOde2yN3eV6tjrZ2rypECNZ50OWOl1+QT/RSulLcWWCICdq5+Ctyklw8ZTDbtPVKKV7MmxdEukLPKEHrTdL2wijisCItAgMBAAECggEAG6WDzmX3f1QjD4OL5r76+7Fz5+J6cQNBmRKYBxWNzEriI4V7T6dtwysAN3QR1mUVUgXaaFy8HA62j8B9qXsvH5/2C70WASh0ddiGJF3dMJTnoKxkYw7ozcswPlZuBmSFdUomoIfWS9yyfUToApU+HNW90Qjoh7F48g8yiK8G3dpsh3wdB0TvVlrjJBi1WhwXXDVNhEtTZslH26U+nq4wbJgg1PB0VIq4wyqWVUqF1BltLj8jS7ViQ2I9uaqSrCMju0vir1cS9SeMSb9vrWI1+oWlsOTmAvVKexxQb6eKW1mIa7Bfqig5fyyc0fa/p5QehGBQ2qtJrUa9Ek4TYE79NwKBgQC+FOpEftbmoQMJQTOS9UxbETBif0mkXXqrbtOuegqw9Wy7AWYOE3sIPHZ15J8PKyjgeTjXRgqdg5zdhxLoVDxv/D1wro7OftiVx4V/cC9oZdeFi4dOUVBn0+FaQt8gXus1l9KUmkp33neVdWoBf1AOk/s9cOn7mE0Cy0zRB66BMwKBgQDi/FK4+T5v4FyWNNz3H/guzlx0nyXkfdwLd+NjT+8u44NxsrNFgDh9/mjEsXbLK82tSMz0/kMjKSCeNa79fRe+suHGUYprGZrMmzDIWvdcfotKpwfNaEQg6Xgqj8wDGDj1xxvCquHOZj1pAlLbwJOT10R2cZyTxiPqu2E0ghGPHwKBgQCisYgqhF4wHJRasYIRQP/P2pCNXeGMW66JWVy5tB++gvJDxdiyJ55g2E+UbNBvzUM5jshGCd9AHsx/GAPo82CfgUidT+tPd2auHI55G26YbsLfvSNct2CY3dO+zAnqzROJVZ+aLc2bd8DnHg5TpcLCF4stdZ3wCNWxlIz1RRvp3QKBgFH1qikAVnsvGD9kdyUEdijwepHhpV0L1RiPAZwqkMLtg9jaHcFKuxtDcbEUI0DZYDrhvp/372YSw6Rc3gLJ2HkTPlLNvp1NcYfPwZ2Wuxq61rDt/vM8Yt0/cBRuN8wmQur8Khnwefh9Ek+Id0LCFoebgy0BePgi43Uuk7rR/GUhAoGANBmfRrXUI9wzWqEpQ53o20M/JKMEacURtKfZ96tpM7rcIQr+7WMT37N4ULs7RU4x7MQtzkzOMSPbFsDQ0Uikmc9ZFhPzCAinjH3/q33pgWGWgGWGN9FwRr8Xua/c7Oqpqz2bNwrnyxwBiZI4F88UXtUP2q89rb/q6iZaJzWpjGI=";

    /**
     * 验证文本密钥配置可以加载为客户端配置。
     */
    @Test
    void shouldLoadTextKeyConfig() {
        OpenApiClientConfig config = MerchantConfigLoader.load();

        // merchant-config.properties 是商户可修改的运行配置，测试只校验加载结果，不绑定某个测试商户。
        assertThat(config.getMerchantId()).isNotBlank();
        assertThat(config.getMerchantJwtSecret()).hasSizeGreaterThanOrEqualTo(32);
        assertThat(config.getPlatformPublicKey()).isNotBlank();
        assertThat(config.getMerchantResponsePrivateKey()).isNotBlank();
        assertThat(config.getBaseUrl()).startsWith("http");
        assertThat(config.getLivemode()).isNotNull();
        assertThat(config.getRawHttpLogEnabled()).isNotNull();
        assertThat(config.getConnectTimeoutMs()).isEqualTo(OpenApiConstants.HTTP_CONNECT_TIMEOUT_MS);
        assertThat(config.getReadTimeoutMs()).isEqualTo(OpenApiConstants.HTTP_READ_TIMEOUT_MS);
    }

    /**
     * 验证平台导出的带 metadata PEM 可被提取为有效 RSA 密钥。
     */
    @Test
    void shouldNormalizeExportedPemWithMetadata() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        String platformPublicPem = "merNo=2607249795\nkeyVersion=v2\n\n"
                + RsaKeyUtils.toPublicKeyPem(config.getPlatformPublicKey());
        String responsePrivatePem = "merNo=2607249795\nkeyVersion=v2\n\n"
                + RsaKeyUtils.toPrivateKeyPem(config.getMerchantResponsePrivateKey());

        assertThat(KeyFileLoader.normalizePem(platformPublicPem)).isEqualTo(config.getPlatformPublicKey());
        assertThat(KeyFileLoader.normalizePem(responsePrivatePem)).isEqualTo(config.getMerchantResponsePrivateKey());
        assertThat(RsaKeyUtils.readPublicKey(platformPublicPem)).isNotNull();
        assertThat(RsaKeyUtils.readPrivateKey(responsePrivatePem)).isNotNull();
    }

    /**
     * 验证商户可不使用 PEM 文件，直接通过配置文本接入请求加密公钥和响应解密私钥。
     */
    @Test
    void shouldSupportInlineTextKeys() throws Exception {
        String platformPublicKey = KeyFileLoader.resolve(null,
                PLATFORM_REQUEST_PUBLIC_KEY_TEXT,
                "payment.gateway.platform-request-public-key");
        String merchantResponsePrivateKey = KeyFileLoader.resolve(null,
                MERCHANT_RESPONSE_PRIVATE_KEY_TEXT,
                "payment.gateway.merchant-response-private-key");
        PublicKey publicKey = RsaKeyUtils.readPublicKey(platformPublicKey);
        PrivateKey privateKey = RsaKeyUtils.readPrivateKey(merchantResponsePrivateKey);
        RSAPrivateCrtKey responsePrivateKey = (RSAPrivateCrtKey) privateKey;
        PublicKey responsePublicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(responsePrivateKey.getModulus(), responsePrivateKey.getPublicExponent()));

        String compact = new OpenApiPayloadCrypto().encrypt("{\"mode\":\"inline-text-key\"}", responsePublicKey);
        String plain = new OpenApiPayloadCrypto().decrypt(compact, privateKey);

        assertThat(publicKey).isNotNull();
        assertThat(platformPublicKey).isEqualTo(PLATFORM_REQUEST_PUBLIC_KEY_TEXT);
        assertThat(merchantResponsePrivateKey).isEqualTo(MERCHANT_RESPONSE_PRIVATE_KEY_TEXT);
        assertThat(plain).contains("inline-text-key");
    }
}
