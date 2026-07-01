package com.scott.payment.sdk;

import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.common.OpenApiEncryptedRequest;
import com.scott.payment.sdk.model.common.OpenApiPayloadParts;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiClientEncryptedPayloadLogTest
 * @date : 2026-07-01 10:28
 * @email : scott_x@163.com
 * @description : SDK 加密 payload 日志拆分测试，负责验证文档联调用的 protectedHeader、header、encryptedAesKey、iv、cipherText 和 tag 字段映射。
 *                本测试不发起真实 HTTP 请求、不修改资金或交易状态；字段值属于密文结构，仅用于沙盒文档核验。
 * @status : create
 */
class OpenApiClientEncryptedPayloadLogTest {

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
        OpenApiPayloadParts components = OpenApiClient.compactPayloadComponentsForLog(compact);

        assertThat(components).isNotNull();
        assertThat(components.getProtectedHeader()).isEqualTo(parts[0]);
        assertThat(components.getEncryptedAesKey()).isEqualTo(parts[1]);
        assertThat(components.getIv()).isEqualTo(parts[2]);
        assertThat(components.getCipherText()).isEqualTo(parts[3]);
        assertThat(components.getTag()).isEqualTo(parts[4]);
        assertThat(components.getHeader()).contains("\"typ\":\"PAYMENT-PAYLOAD\"")
                .contains("\"alg\":\"RSA-OAEP-256\"")
                .contains("\"enc\":\"A256GCM\"");
        assertThat(OpenApiClient.compactPayloadComponentsForLog("invalid")).isNull();
    }

    /**
     * 验证调试文档使用的 HTTP 请求日志可以按对象输出 body，避免把 JSON body 作为转义字符串写入日志示例。
     */
    @Test
    void shouldKeepHttpBodyAsObjectForDebugLog() {
        OpenApiEncryptedRequest request = OpenApiEncryptedRequest.builder()
                .livemode(false)
                .data("header.key.iv.cipher.tag")
                .build();
        Map<String, Object> httpRequest = new LinkedHashMap<String, Object>();
        httpRequest.put("method", "POST");
        httpRequest.put("body", request);
        httpRequest.put("headers", OpenApiLogSanitizer.sanitizeHeaders(headers()));

        String json = JsonSupport.toLogJson(httpRequest);

        assertThat(json).contains("\"body\":{\"livemode\":false");
        assertThat(json).doesNotContain("\"body\":\"{");
        assertThat(json).contains("Bearer abcdefghij***uvwxyz");
    }

    /**
     * 验证日志 JSON 会过滤 null 字段，避免商户联调时看到大量无效参数。
     */
    @Test
    void shouldExcludeNullFieldsFromLogJson() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("tradeNo", "pay_123");
        payload.put("redirectUrl", null);

        String json = JsonSupport.toLogJson(payload);

        assertThat(json).contains("\"tradeNo\":\"pay_123\"");
        assertThat(json).doesNotContain("redirectUrl");
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer abcdefghijklmnopqrstuvwxyz");
        headers.put("Accept", "application/json");
        return headers;
    }
}
