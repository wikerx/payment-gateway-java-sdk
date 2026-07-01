package com.scott.payment.sdk.logging;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiLogSanitizerTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 日志辅助测试，负责验证卡号、账户号、Authorization 和 body 摘要等日志脱敏行为。
 *                本测试不涉及资金变更、密钥轮换或外部渠道调用，只校验日志输出边界。
 * @status : modify
 */
class OpenApiLogSanitizerTest {

    /**
     * 验证商户号明文输出，卡号和账户号按敏感数据处理。
     */
    @Test
    void shouldMaskSensitiveNumbers() {
        assertThat(OpenApiLogSanitizer.maskMerchantId("200046999")).isEqualTo("200046999");
        assertThat(OpenApiLogSanitizer.maskCardNo("4111111111111111")).isEqualTo("411111******1111");
        assertThat(OpenApiLogSanitizer.maskAccountNo("123456789")).isEqualTo("***6789");
    }

    /**
     * 验证 Header 日志脱敏 Authorization，避免完整 JWT 进入普通日志。
     */
    @Test
    void shouldMaskAuthorizationHeader() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer abcdefghijklmnopqrstuvwxyz");
        headers.put("Accept", "application/json");

        Map<String, String> sanitized = OpenApiLogSanitizer.sanitizeHeaders(headers);

        assertThat(sanitized.get("Authorization")).isEqualTo("Bearer abcdefghij***uvwxyz");
        assertThat(sanitized.get("Accept")).isEqualTo("application/json");
    }

    /**
     * 验证 body 日志只输出摘要。
     */
    @Test
    void shouldSummarizeBody() {
        assertThat(OpenApiLogSanitizer.bodySummary("{\"data\":\"secret\"}"))
                .contains("length=", "encryptedData=true")
                .doesNotContain("secret");
        assertThat(OpenApiLogSanitizer.bodySummary(null)).isEqualTo("null");
    }

    /**
     * 验证递归脱敏对象时会移除 null 字段，避免日志里出现大量无效参数。
     */
    @Test
    void shouldRemoveNullFieldsWhenSanitizingObject() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("tradeNo", "pay_123");
        payload.put("redirectUrl", null);

        Object sanitized = OpenApiLogSanitizer.sanitizeObject(payload);

        assertThat(sanitized).isInstanceOf(Map.class);
        Map<?, ?> sanitizedMap = (Map<?, ?>) sanitized;
        assertThat(sanitizedMap.get("tradeNo")).isEqualTo("pay_123");
        assertThat(sanitizedMap.containsKey("redirectUrl")).isFalse();
    }
}
