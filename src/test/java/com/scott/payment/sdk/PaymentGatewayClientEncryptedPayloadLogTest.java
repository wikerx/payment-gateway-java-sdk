package com.scott.payment.sdk;

import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentGatewayClientEncryptedPayloadLogTest
 * @date : 2026-07-01 10:28
 * @email : scott_x@163.com
 * @description : SDK 加密 payload 日志拆分测试，负责验证文档联调用的 protectedHeader、header、encryptedAesKey、iv、cipherText 和 tag 字段映射。
 *                本测试不发起真实 HTTP 请求、不修改资金或交易状态；字段值属于密文结构，仅用于沙盒文档核验。
 * @status : create
 */
class PaymentGatewayClientEncryptedPayloadLogTest {

    /**
     * 验证 SDK 调试日志使用的 compact payload 拆分字段，便于商户文档对齐 header、encryptedAesKey、iv、cipherText 和 tag。
     */
    @Test
    void shouldSplitCompactPayloadComponentsForDebugLog() throws Exception {
        OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String compact = crypto.encrypt("{\"orderNo\":\"ORDER-DEBUG\"}", keyPair.getPublic());
        String[] parts = compact.split("\\.");
        Map<String, String> components = PaymentGatewayClient.compactPayloadComponentsForLog(compact);

        assertThat(components).containsEntry("protectedHeader", parts[0])
                .containsEntry("encryptedAesKey", parts[1])
                .containsEntry("iv", parts[2])
                .containsEntry("cipherText", parts[3])
                .containsEntry("tag", parts[4]);
        assertThat(components.get("header")).contains("\"typ\":\"PAYMENT-PAYLOAD\"")
                .contains("\"alg\":\"RSA-OAEP-256\"")
                .contains("\"enc\":\"A256GCM\"");
        assertThat(PaymentGatewayClient.compactPayloadComponentsForLog("invalid")).isEmpty();
    }
}
