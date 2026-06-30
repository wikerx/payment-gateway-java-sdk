package com.wikerx.payment.gateway.sdk.config;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.crypto.OpenApiPayloadCrypto;
import com.wikerx.payment.gateway.sdk.crypto.RsaKeyUtils;
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
 * @email : scott_x@163.com
 * @description : 商户配置加载测试，负责验证 SDK 只读取 merchant-config.properties，并能解析 2606177036 沙盒商户的文本密钥和 PEM 文件。
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
    private static final String PLATFORM_REQUEST_PUBLIC_KEY_TEXT = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt543PuLAAQ4KhVOvhqnv18SWnQ8gsd5EzbEwMPPiCEtYDZYPT4TntgoIE+w5Y4YKs8czMicI1y/FA7zR9wQCNhUfY4BE59/ZIRXBb5SzlaW7Ci0RL0Z1YdbceetOWT4766VoRTQcv3dFIE0uRVv8K++pnVSYmFYJ3N+sDRyoeHyhP2t2UIZdvlYwql7uFenJdwUgZqxTnDM5Eea9xKgufympD2dG3ITqoB3ocAyPbv0DpPTgEx4vSZlr6sJfUPimGZHL+kK32jAPjjJPhlxyjEXAWJq4QU5lkRKVKEmiYuvY1c8VWG7tL/ZQTnQNLApcg8IA1rHb0722zDwPQRcouwIDAQAB";

    /**
     * 商户解密平台响应私钥文本，PKCS#8 DER Base64 格式。
     *
     * 敏感字段：是。
     * 是否允许为空：否。
     * 用途：验证 SDK 支持商户直接通过配置文本接入响应解密私钥；测试不得输出该字段。
     */
    private static final String MERCHANT_RESPONSE_PRIVATE_KEY_TEXT = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCwL+Tqkosubupv5Mv8Y2vDRebudT8BmjfP6O7le8z4EqZokTsgx9VKB2GgtpTIJhr9+LPN26aBrrKE+3C+TVBopM38yrmJb2mz+pMg5Sc6i1WmOReyjaUzm7Y1a/RdsgjAA/xhGCP99+m8CqaXzacwGurd4UR287EJFT+f4bD8VYdns9l2xTOEFh1SHWjkqbQ4LvA2I6lUtet5VbHGrxe3cYjUSNbXpNxO/9A5e8W5fCA2fkdsmotCrpIxGylb9m2O586zqqQJE7y4CfbqwiuRM8KEjVSD0e5dBbRn/GDKWT+6hEHExVCGsDUf69XE+K5qCH9ENzLMa8GhV4GJ1cvBAgMBAAECggEACs8TZY8yPfnkxNLV72DXlYIqN96mTAY+rpEJJ/fDhEQbpK5lQN51n74qb4JICCvrS8G4YTScp1fVAKrO2MP0Pwk+UGgNACkfRGEG6fWZBaLS8TjPMv9030DKKcaa1i00E3ijIP9KxPvSwXwrSoSdwUojdtOJQF10styUe83sLlzEWcM5e/4fPD8cJrAheJfcZNks19wcCfpH2JVWEoMqZxNR3+iG4+iVaHNr56LCfXXgQrjaOxIG0Esfb6PDE8aG+5Hdpqv+bMYgXZbpfFG+wO7sLktlY58j54pjKxvMapD10BDSHhgDj2MG+Qe9IWY5kNnlqWe7wx2ZpaTgIeq9mQKBgQDO+ana4z/9pHfDDWZ3s4eMr5wa+69zMOjd7ujrzThjM2tB0vXFvPtuR7Eu7UffBHuYaEXDHZjjyyCYiW0OxJX/Nx++wHZKoBZd8HMiVHippZGMFf1iky5FHOqBtfdoiLbI/bmSeK25jwr4ppK6hLT2gvYUHSBttQbthBw9Q6z86QKBgQDZ61UKKJva5yphklotpFOCPo8MVX5ZhRQ1kzMyPe1tKgjHl2/PvcZCyQoEYAixIx1oTkHhK7U/YBBUEWkFrkSZF543R+L2dIjwCnNeU3cFKQquwe97EFaFbnj1C6fEy11GvD31acVM2Ar18CNVW8u7NW4nGakwLkzxYzqo/fSxGQKBgC0bllwhCNIzpPI6mmleFB2iLChpT9yP/UBZECRL7o0YKLkIzA2TWUy4jTIH/pDpPjKCDyot6iNDItB7quv8BiDAF8gP0/gBmb5RaBZESKPYdLcOF9IC96OSYL1yNgBvQz7cpTP53wrA1QhJ7VJ/F51d/1l025ttR5w+HZVwiWP5AoGAevd9bBcQI0zwMFC6TCj+6m7Mn4QaoP8kMTsX15D0SfY/MAk3Eb2fg44X9fIO1Y3gCTynlhzo0JMvg5Czd34nvU+DeuQ4oSOPJgxvn1lvvtyy53wN256ThAWbgYMLL1QmFUUhnTsLF1qNjMvt1DvRUZlLyAqF2uc1ibyZnER5b/kCgYEAubCdmTZwgOQgM8TZWXHOFD2JF5QEgRKgGBAaNQyAsP9rCWLMs3GiMfeJZt/iCCU54Ax+Jebm8JEjRcRNzQs7cNaHo6JsdiGDzIHguiLsLAvzcYk4vJ70qVzx2AX/7eGkHVvvl1PbW+LozC+uZiw82PvQuLcZWBKNOd0UGmXHFUA=";

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
        assertThat(config.getRawHttpLogEnabled()).isTrue();
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
