package com.wikerx.payment.gateway.sdk.logging;

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
 * @description : OpenAPI SDK 日志脱敏测试，负责验证商户号、卡号、账户号、Authorization 和 body 摘要不会泄露完整敏感数据。
 *                本测试不涉及资金变更、密钥轮换或外部渠道调用，只校验日志输出边界。
 * @status : modify
 */
class PaymentGatewayLogSanitizerTest {

    /**
     * 验证日志脱敏工具处理敏感值。
     */
    @Test
    void shouldMaskSensitiveValues() {
        assertThat(PaymentGatewayLogSanitizer.maskMerchantId("200046999")).isEqualTo("200***999");
        assertThat(PaymentGatewayLogSanitizer.maskCardNo("4111111111111111")).isEqualTo("411111******1111");
        assertThat(PaymentGatewayLogSanitizer.maskAccountNo("123456789")).isEqualTo("*****6789");
    }

    /**
     * 验证 Header 日志不会输出完整 Authorization。
     */
    @Test
    void shouldSanitizeAuthorizationHeader() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer abcdefghijklmnopqrstuvwxyz");
        headers.put("Accept", "application/json");

        Map<String, String> sanitized = PaymentGatewayLogSanitizer.sanitizeHeaders(headers);

        assertThat(sanitized.get("Authorization")).isEqualTo("Bearer abcdef******wxyz");
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
