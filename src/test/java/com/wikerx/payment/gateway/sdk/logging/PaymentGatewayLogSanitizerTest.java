package com.wikerx.payment.gateway.sdk.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
