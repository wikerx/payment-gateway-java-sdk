package com.scott.payment.sdk.client;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import com.scott.payment.sdk.testkit.CapturingOpenApiTransport;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import com.scott.payment.sdk.json.JsonSupport;
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
    void createCustomer_shouldSuccess() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setFirstname("Ada");
        request.setLastname("Lovelace");
        request.setEmail("ada@example.com");
        request.setPhone("+12025550123");
        request.setCountry("US");

        log.info("用例开始: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "CustomerCreateTest",
                "apiName", "Customer Create",
                "request", request,
                "country", request.getCountry(),
                "emailDomain", "example.com")));
        OpenApiResult<CustomerResponse> result = client.createCustomer(request);
        log.info("用例结果: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "CustomerCreateTest",
                "apiName", "Customer Create",
                "success", result.isSuccess(),
                "data", result.getData(),
                "requestPath", transport.getLastRequest().getUri().getPath())));

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/mer/customers");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastRequest().getBody()).contains("\"data\"");
        assertThat(transport.getLastRequest().getBody()).doesNotContain("ada@example.com", "+12025550123");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getCustomerId()).isNotBlank();
    }
}
