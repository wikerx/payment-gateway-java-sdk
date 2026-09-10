package com.scott.payment.sdk.api.customers;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerRetrieveTest
 * @date : 2026-07-02 18:36
 * @email : scott-***@163.com
 * @description : 检索客户接口真实网关调用 case，负责先创建一个沙盒客户，再向测试网关发起
 *                /pay-api/mer/customers/{customerId} 的 GET 查询请求。本 case 只读取客户资料，不修改客户、交易或资金状态；
 *                响应可能包含个人信息，日志会通过 OpenApiLogSanitizer 脱敏后输出。
 * @status : create
 */
@Slf4j
public class CustomerInquiryTest {

    /**
     * 真实请求测试环境网关检索客户。
     *
     * 该方法使用 GET + Bearer JWT 调用客户检索接口，不发送请求体；响应 data 由 SDK 使用商户响应私钥解密。
     */
    @Test
    public void testCustomerInquiry() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        String customerId = "cus_ysNthtSsNGrwje3";
//        String customerId = CustomerApiTestSupport.createCustomerForCase(client);

        log.info("检索客户真实调用-请求参数: {}", JsonSupport.toLogJson(CustomerApiTestSupport.logFields(
                "customerId", customerId,
                "requestPath", "/pay-api/mer/customers/" + customerId)));
        OpenApiResult<CustomerResponse> result = client.retrieveCustomer(customerId);
        log.info("检索客户真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getCustomerId()).isEqualTo(customerId);
    }
}
