package com.wikerx.payment.gateway.sdk.config;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.crypto.RsaKeyUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : MerchantConfigLoaderTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 商户配置加载测试，负责验证 SDK 只读取 merchant-config.properties，并能解析 2606177036 沙盒商户的文本密钥和 PEM 文件。
 *                本测试不输出 API 私钥或商户响应私钥明文，不新增数据库数据，也不发起真实网关请求。
 * @status : modify
 */
class MerchantConfigLoaderTest {

    /**
     * 验证文本密钥配置可以加载为客户端配置。
     */
    @Test
    void shouldLoadTextKeyConfig() {
        PaymentGatewayClientConfig config = MerchantConfigLoader.load();

        assertThat(config.getMerchantId()).isEqualTo("2606177036");
        assertThat(config.getMerchantJwtSecret()).hasSizeGreaterThanOrEqualTo(32);
        assertThat(config.getPlatformPublicKey()).isNotBlank();
        assertThat(config.getMerchantResponsePrivateKey()).isNotBlank();
        assertThat(config.getBaseUrl()).isEqualTo("http://localhost:58060");
        assertThat(config.getLivemode()).isFalse();
        assertThat(config.getConnectTimeoutMs()).isEqualTo(3000);
        assertThat(config.getReadTimeoutMs()).isEqualTo(10000);
    }

    /**
     * 验证平台导出的带 metadata PEM 可被提取为有效 RSA 密钥。
     */
    @Test
    void shouldNormalizeExportedPemWithMetadata() {
        PaymentGatewayClientConfig config = MerchantConfigLoader.load();
        String platformPublicPem = "merNo=2606177036\nkeyVersion=v2\n\n"
                + RsaKeyUtils.toPublicKeyPem(config.getPlatformPublicKey());
        String responsePrivatePem = "merNo=2606177036\nkeyVersion=v2\n\n"
                + RsaKeyUtils.toPrivateKeyPem(config.getMerchantResponsePrivateKey());

        assertThat(KeyFileLoader.normalizePem(platformPublicPem)).isEqualTo(config.getPlatformPublicKey());
        assertThat(KeyFileLoader.normalizePem(responsePrivatePem)).isEqualTo(config.getMerchantResponsePrivateKey());
        assertThat(RsaKeyUtils.readPublicKey(platformPublicPem)).isNotNull();
        assertThat(RsaKeyUtils.readPrivateKey(responsePrivatePem)).isNotNull();
    }
}
