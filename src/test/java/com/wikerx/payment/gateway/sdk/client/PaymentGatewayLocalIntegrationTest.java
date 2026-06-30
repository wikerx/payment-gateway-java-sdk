package com.wikerx.payment.gateway.sdk.client;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClient;
import com.wikerx.payment.gateway.sdk.PaymentGatewayResult;
import com.wikerx.payment.gateway.sdk.model.balance.BalanceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test002 本地网关基础数据测试。
 */
@EnabledIfSystemProperty(named = "payment.gateway.local-it", matches = "true")
class PaymentGatewayLocalIntegrationTest {

    /**
     * 本机忽略文件中的 Test002 商户配置。
     */
    private static final String LOCAL_CONFIG_FILE = "merchant-config-test002.properties";

    /**
     * 调用本地 58060 网关查询 USD 账户基础数据。
     */
    @Test
    void shouldRetrieveUsdBalanceFromLocalGateway() {
        PaymentGatewayClient client = PaymentGatewayClient.create(LOCAL_CONFIG_FILE);

        PaymentGatewayResult<List<BalanceResponse>> result = client.retrieveBalances("USD");

        assertThat(result.getCode()).isZero();
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().get(0).getCurrency()).isEqualTo("USD");
    }
}
