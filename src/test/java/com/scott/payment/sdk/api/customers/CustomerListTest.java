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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerListTest
 * @date : 2026-07-02 18:36
 * @email : scott-***@163.com
 * @description : 列出所有客户接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/mer/customers 的 GET 请求。本 case 只读取客户列表，不创建、不更新、不删除客户资料；
 *                响应可能包含个人信息，日志会通过 OpenApiLogSanitizer 脱敏后输出。
 * @status : create
 */
@Slf4j
public class CustomerListTest {

    /**
     * 真实请求测试环境网关列出客户。
     *
     * 该方法使用 GET + Bearer JWT 调用客户列表接口，不发送请求体；响应 data 由 SDK 解密为客户列表。
     */
    @Test
    public void testCustomerList() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);

        log.info("列出所有客户真实调用-请求参数: {}", JsonSupport.toLogJson(CustomerApiTestSupport.logFields(
                "requestPath", "/pay-api/mer/customers")));
        OpenApiResult<List<CustomerResponse>> result = client.listCustomers();
        log.info("列出所有客户真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }
}
