package com.wikerx.payment.gateway.sdk.config;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.crypto.RsaKeyUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantConfigLoaderTest {

    /**
     * 验证文本密钥配置可以加载为客户端配置。
     */
    @Test
    void shouldLoadTextKeyConfig() {
        PaymentGatewayClientConfig config = MerchantConfigLoader.load("merchant-config.properties");

        assertThat(config.getMerchantId()).isEqualTo("200046");
        assertThat(config.getMerchantJwtSecret()).hasSizeGreaterThanOrEqualTo(32);
        assertThat(config.getPlatformPublicKey()).isNotBlank();
        assertThat(config.getMerchantResponsePrivateKey()).isNotBlank();
        assertThat(config.getBaseUrl()).isEqualTo("https://payment-gateway.example.com");
    }

    /**
     * 验证平台导出的带 metadata PEM 可被提取为有效 RSA 密钥。
     */
    @Test
    void shouldNormalizeExportedPemWithMetadata() {
        PaymentGatewayClientConfig config = MerchantConfigLoader.load("merchant-config.properties");
        String platformPublicPem = "merNo=Test002\nkeyVersion=v2\n\n"
                + RsaKeyUtils.toPublicKeyPem(config.getPlatformPublicKey());
        String responsePrivatePem = "merNo=Test002\nkeyVersion=v2\n\n"
                + RsaKeyUtils.toPrivateKeyPem(config.getMerchantResponsePrivateKey());

        assertThat(KeyFileLoader.normalizePem(platformPublicPem)).isEqualTo(config.getPlatformPublicKey());
        assertThat(KeyFileLoader.normalizePem(responsePrivatePem)).isEqualTo(config.getMerchantResponsePrivateKey());
        assertThat(RsaKeyUtils.readPublicKey(platformPublicPem)).isNotNull();
        assertThat(RsaKeyUtils.readPrivateKey(responsePrivatePem)).isNotNull();
    }
}
