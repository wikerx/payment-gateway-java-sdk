package com.scott.payment.sdk.api.customers;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerCreateTest
 * @date : 2026-07-02 18:36
 * @email : scott_x@163.com
 * @description : 创建客户接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/mer/customers 请求。本 case 会真实创建沙盒客户资料，涉及姓名、邮箱、电话、地址和证件号等敏感数据；
 *                SDK 会按最新 OpenAPI 协议完成 JWT 签名、请求加密、HTTP 调用和响应解密。本 case 不负责商户本地客户幂等、
 *                KYC、状态流转或外部渠道同步。
 * @status : create
 */
@Slf4j
public class CustomerCreateTest {

    /**
     * 真实请求测试环境网关创建客户。
     *
     * 该方法通过默认 JDK HTTP Transport 发起真实 POST 请求，重复运行会生成唯一邮箱和证件号，避免沙盒客户资料冲突。
     */
    @Test
    public void testCustomerCreate() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        CustomerCreateRequest request = CustomerApiTestSupport.createRequest();

        log.info("创建客户真实调用-请求原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
        OpenApiResult<CustomerResponse> result = client.createCustomer(request);
        log.info("创建客户真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getCustomerId()).isNotBlank();
    }
}
