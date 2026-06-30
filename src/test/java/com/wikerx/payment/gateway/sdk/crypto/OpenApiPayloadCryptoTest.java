package com.wikerx.payment.gateway.sdk.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wikerx.payment.gateway.sdk.json.JsonSupport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiPayloadCryptoTest {

    /**
     * 验证 compact payload 可加解密且不输出 kid。
     */
    @Test
    void shouldEncryptAndDecryptCompactPayloadWithoutKid() throws Exception {
        OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String compact = crypto.encrypt("{\"orderNo\":\"ORDER-1\"}", publicKey);
        String plain = crypto.decrypt(compact, privateKey);
        String headerJson = new String(Base64.getUrlDecoder().decode(compact.split("\\.")[0]), StandardCharsets.UTF_8);
        Map<String, Object> header = JsonSupport.fromJson(headerJson, new TypeReference<Map<String, Object>>() {
        });

        assertThat(plain).contains("ORDER-1");
        assertThat(compact.split("\\.")).hasSize(5);
        assertThat(header).containsEntry("typ", "PAYMENT-PAYLOAD")
                .containsEntry("alg", "RSA-OAEP-256")
                .containsEntry("enc", "A256GCM");
        assertThat(header).doesNotContainKey("kid");
    }
}
