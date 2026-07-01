package com.scott.payment.sdk.logging;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentGatewayLogSanitizerTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 日志辅助测试，负责验证卡号脱敏、Header 原值输出和 body 摘要行为。
 *                本测试不涉及资金变更、密钥轮换或外部渠道调用，只校验日志输出边界。
 * @status : modify
 */
class PaymentGatewayLogSanitizerTest {

    /**
     * 验证只有卡号保持脱敏，商户号和账户号按联调要求明文输出。
     */
    @Test
    void shouldOnlyMaskCardNumber() {
        assertThat(PaymentGatewayLogSanitizer.maskMerchantId("200046999")).isEqualTo("200046999");
        assertThat(PaymentGatewayLogSanitizer.maskCardNo("4111111111111111")).isEqualTo("411111******1111");
        assertThat(PaymentGatewayLogSanitizer.maskAccountNo("123456789")).isEqualTo("123456789");
    }

    /**
     * 验证 Header 日志按原值输出 Authorization，方便商户核验 JWT。
     */
    @Test
    void shouldKeepAuthorizationHeaderOriginal() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer abcdefghijklmnopqrstuvwxyz");
        headers.put("Accept", "application/json");

        Map<String, String> sanitized = PaymentGatewayLogSanitizer.sanitizeHeaders(headers);

        assertThat(sanitized.get("Authorization")).isEqualTo("Bearer abcdefghijklmnopqrstuvwxyz");
        assertThat(sanitized.get("Accept")).isEqualTo("application/json");
    }

    /**
     * 验证 body 日志只输出摘要。
     */
    @Test
    void shouldSummarizeBody() {
        assertThat(PaymentGatewayLogSanitizer.bodySummary("{\"data\":\"secret\"}"))
                .contains("length=", "encryptedData=true")
                .doesNotContain("secret");
        assertThat(PaymentGatewayLogSanitizer.bodySummary(null)).isEqualTo("null");
    }
}
