package com.scott.payment.sdk.client;

import com.scott.payment.sdk.PaymentGatewayClient;
import com.scott.payment.sdk.PaymentGatewayResult;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import com.scott.payment.sdk.testkit.CapturingPaymentGatewayTransport;
import com.scott.payment.sdk.testkit.PaymentGatewayTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerCreateTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 创建客户接口商户调用 case，演示客户姓名、邮箱、手机号和国家信息的组装方式。
 *                本 case 涉及客户联系方式等敏感数据和 OpenAPI 请求加密，只输出邮箱域名级摘要，不输出完整客户资料明文。
 * @status : create
 */
@Slf4j
class CustomerCreateTest {

    /**
     * 验证创建客户接口使用最新 OpenAPI 加密协议。
     */
    @Test
    void createCustomer() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setFirstname("Ada");
        request.setLastname("Lovelace");
        request.setEmail("ada@example.com");
        request.setPhone("+12025550123");
        request.setCountry("US");

        log.info("创建客户 case 开始：country={} emailDomain=example.com", request.getCountry());
        PaymentGatewayResult<CustomerResponse> result = client.createCustomer(request);
        log.info("创建客户 case 结果：success={} customerId={} requestPath={}",
                result.isSuccess(), result.getData().getCustomerId(), transport.getLastRequest().getUri().getPath());

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/mer/customers");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastRequest().getBody()).contains("\"data\"");
        assertThat(transport.getLastRequest().getBody()).doesNotContain("ada@example.com", "+12025550123");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getCustomerId()).isNotBlank();
    }
}
