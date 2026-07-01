package com.scott.payment.sdk.model;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.model.common.AchDebitPaymentMethodData;
import com.scott.payment.sdk.model.common.CardPaymentMethodData;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : SensitiveModelToStringTest
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : 敏感模型 toString 测试，负责验证卡号、CVC、银行账号、证件号和密钥类字段不会通过 Lombok toString 输出。
 *                本测试不发起 HTTP 请求、不执行加密传输、不修改资金或交易状态；测试数据仅用于本地脱敏断言。
 * @status : modify
 */
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
        OpenApiClientConfig config = OpenApiClientConfig.builder()
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
