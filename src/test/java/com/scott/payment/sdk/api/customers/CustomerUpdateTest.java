package com.scott.payment.sdk.api.customers;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import com.scott.payment.sdk.model.customer.CustomerUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerUpdateTest
 * @date : 2026-07-02 18:36
 * @email : scott_x@163.com
 * @description : 更新客户接口真实网关调用 case，负责先创建一个沙盒客户，再向测试网关发起
 *                /pay-api/mer/customers/{customerId} 的 PUT 更新请求。本 case 会真实修改沙盒客户资料，涉及个人信息和证件号等敏感数据；
 *                SDK 会按最新 OpenAPI 协议完成请求加密和响应解密。本 case 不负责商户本地客户资料同步、KYC、状态流转或外部渠道同步。
 * @status : create
 */
@Slf4j
public class CustomerUpdateTest {

    /**
     * 真实请求测试环境网关更新客户。
     *
     * 该方法先创建前置客户，随后使用返回的 customerId 发起真实 PUT 请求；路径参数不进入请求体，请求体按 OpenAPI data 加密。
     */
    @Test
    public void testCustomerUpdate() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        String customerId = "cus_ysNthtSsNGrwje3";
//        String customerId = CustomerApiTestSupport.createCustomerForCase(client);
        CustomerUpdateRequest request = CustomerApiTestSupport.updateRequest();

        log.info("更新客户真实调用-请求参数: {}", JsonSupport.toLogJson(CustomerApiTestSupport.logFields(
                "customerId", customerId,
                "requestPath", "/pay-api/mer/customers/" + customerId)));
        log.info("更新客户真实调用-请求原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
        OpenApiResult<CustomerResponse> result = client.updateCustomer(customerId, request);
        log.info("更新客户真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getCustomerId()).isEqualTo(customerId);
    }
}
