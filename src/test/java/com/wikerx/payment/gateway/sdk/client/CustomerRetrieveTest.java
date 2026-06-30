package com.wikerx.payment.gateway.sdk.client;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClient;
import com.wikerx.payment.gateway.sdk.PaymentGatewayResult;
import com.wikerx.payment.gateway.sdk.model.customer.CustomerResponse;
import com.wikerx.payment.gateway.sdk.testkit.CapturingPaymentGatewayTransport;
import com.wikerx.payment.gateway.sdk.testkit.PaymentGatewayTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerRetrieveTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 检索客户接口商户调用 case，演示按 customerId 查询当前商户名下客户资料。
 *                本 case 不修改资金或客户状态，但响应包含客户敏感信息；测试只输出客户号和请求路径摘要。
 * @status : create
 */
@Slf4j
class CustomerRetrieveTest {

    /**
     * 验证检索客户接口使用 Bearer JWT 并解密响应 data。
     */
    @Test
    void retrieveCustomer() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);

        log.info("检索客户 case 开始：customerId=cus_123");
        PaymentGatewayResult<CustomerResponse> result = client.retrieveCustomer("cus_123");
        log.info("检索客户 case 结果：success={} customerId={} requestPath={}",
                result.isSuccess(), result.getData().getCustomerId(), transport.getLastRequest().getUri().getPath());

        assertThat(transport.getLastRequest().getMethod()).isEqualTo("GET");
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/mer/customers/cus_123");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getCustomerId()).isEqualTo("cus_123");
    }
}
