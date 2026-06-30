package com.wikerx.payment.gateway.sdk.client;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClient;
import com.wikerx.payment.gateway.sdk.PaymentGatewayResult;
import com.wikerx.payment.gateway.sdk.json.JsonSupport;
import com.wikerx.payment.gateway.sdk.model.balance.BalanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test002 本地网关基础数据测试。
 */
@Slf4j
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
        String configFile = System.getProperty("payment.gateway.local-config", LOCAL_CONFIG_FILE);
        PaymentGatewayClient client = PaymentGatewayClient.create(configFile);
        log.info("PaymentGatewayClient initialized successfully.");

        PaymentGatewayResult<List<BalanceResponse>> result = client.retrieveBalances("USD");
        log.info("result:{}" , JsonSupport.toJson(result));

        assertThat(result.getCode()).isZero();
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().get(0).getCurrency()).isEqualTo("USD");
    }
}
