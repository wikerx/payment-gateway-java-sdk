package com.wikerx.payment.gateway.sdk.model;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClientConfig;
import com.wikerx.payment.gateway.sdk.model.common.AchDebitPaymentMethodData;
import com.wikerx.payment.gateway.sdk.model.common.CardPaymentMethodData;
import com.wikerx.payment.gateway.sdk.model.customer.CustomerCreateRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveModelToStringTest {

    /**
     * 验证敏感模型字段不会进入 toString。
     */
    @Test
    void shouldExcludeSensitiveFieldsFromToString() {
        CardPaymentMethodData card = new CardPaymentMethodData();
        card.setNumber("4111111111111111");
        card.setCvc("123");
        AchDebitPaymentMethodData ach = new AchDebitPaymentMethodData();
        ach.setBankAccountNo("123456789");
        CustomerCreateRequest customer = new CustomerCreateRequest();
        customer.setIdentityNo("ID123456");
        PaymentGatewayClientConfig config = PaymentGatewayClientConfig.builder()
                .merchantJwtSecret("jwt-secret")
                .platformPublicKey("public-key")
                .merchantResponsePrivateKey("private-key")
                .build();

        assertThat(card.toString()).doesNotContain("4111111111111111", "123");
        assertThat(ach.toString()).doesNotContain("123456789");
        assertThat(customer.toString()).doesNotContain("ID123456");
        assertThat(config.toString()).doesNotContain("jwt-secret", "public-key", "private-key");
    }
}
