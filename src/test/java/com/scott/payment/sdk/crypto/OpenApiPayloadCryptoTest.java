package com.scott.payment.sdk.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.model.common.OpenApiPayloadParts;
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

    /**
     * 验证 SDK 对外公开的 compact payload 拆分方法可直接获取 encryptedAesKey、iv、cipherText 和 tag。
     */
    @Test
    void shouldSplitCompactPayloadPartsForMerchantReference() throws Exception {
        OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        OpenApiPayloadParts encryptedParts = crypto.encryptToParts("{\"orderNo\":\"ORDER-PARTS\"}", keyPair.getPublic());
        OpenApiPayloadParts splitParts = crypto.splitCompactPayload(encryptedParts.toCompactPayload());

        assertThat(splitParts.getProtectedHeader()).isEqualTo(encryptedParts.getProtectedHeader());
        assertThat(splitParts.getHeader()).contains("\"typ\":\"PAYMENT-PAYLOAD\"");
        assertThat(splitParts.getEncryptedAesKey()).isEqualTo(encryptedParts.getEncryptedAesKey());
        assertThat(splitParts.getIv()).isEqualTo(encryptedParts.getIv());
        assertThat(splitParts.getCipherText()).isEqualTo(encryptedParts.getCipherText());
        assertThat(splitParts.getTag()).isEqualTo(encryptedParts.getTag());
        assertThat(crypto.decrypt(splitParts.toCompactPayload(), keyPair.getPrivate())).contains("ORDER-PARTS");
    }
}
