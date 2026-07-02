package com.scott.payment.sdk.api.customers;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerDeleteTest
 * @date : 2026-07-02 18:36
 * @email : scott_x@163.com
 * @description : 删除客户接口真实网关调用 case，负责先创建一个沙盒客户，再向测试网关发起
 *                /pay-api/mer/customers/{customerId} 的 DELETE 请求。本 case 会真实删除网关侧沙盒客户资料，网关响应 data 为 boolean；
 *                不删除商户本地客户、订单、支付、退款、代付或对账记录，也不负责客户删除幂等和审计策略。
 * @status : create
 */
@Slf4j
public class CustomerDeleteTest {

    /**
     * 真实请求测试环境网关删除客户。
     *
     * 该方法使用 DELETE + Bearer JWT 调用客户删除接口，不发送请求体；响应 data 由 SDK 自动解密。
     */
    @Test
    public void testCustomerDelete() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        String customerId = "cus_ysNthtSsNGrwje3";
//        String customerId = CustomerApiTestSupport.createCustomerForCase(client);

        log.info("删除客户真实调用-请求参数: {}", JsonSupport.toLogJson(CustomerApiTestSupport.logFields(
                "customerId", customerId,
                "requestPath", "/pay-api/mer/customers/" + customerId)));
        OpenApiResult<Boolean> result = client.deleteCustomer(customerId);
        log.info("删除客户真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isTrue();
    }
}
