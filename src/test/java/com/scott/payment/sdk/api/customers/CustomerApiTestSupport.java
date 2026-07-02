package com.scott.payment.sdk.api.customers;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import com.scott.payment.sdk.model.customer.CustomerUpdateRequest;
import com.scott.payment.sdk.util.OrderNoGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerApiTestSupport
 * @date : 2026-07-02 18:36
 * @email : scott_x@163.com
 * @description : 客户 API 真实网关测试辅助类，负责为创建、更新、检索、删除和列表 case 构造唯一测试客户资料。
 *                本类只服务 SDK 测试示例，不直接代表商户生产代码；构造的请求包含邮箱、电话、地址和证件号等敏感字段，
 *                调用方输出日志前必须继续使用 OpenApiLogSanitizer 脱敏。本类不负责客户幂等落库、KYC、状态流转或外部渠道同步。
 * @status : create
 */
final class CustomerApiTestSupport {

    /**
     * 测试邮箱域名。
     *
     * 是否敏感：否。
     * 用途：生成沙盒客户邮箱，避免重复运行时邮箱冲突。
     */
    private static final String TEST_EMAIL_DOMAIN = "@test.com";

    private CustomerApiTestSupport() {
    }

    /**
     * 构造客户创建请求。
     *
     * 该方法只组装测试客户资料，不发起 HTTP 请求、不执行签名加密、不修改客户或资金状态。
     *
     * @return 客户创建请求
     */
    static CustomerCreateRequest createRequest() {
        String suffix = OrderNoGenerator.generate("CUS_");
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setFirstname("Lily");
        request.setLastname("Brown");
        request.setEmail("lily_brown_" + suffix + TEST_EMAIL_DOMAIN);
        request.setPhone("13628173752");
        request.setIdentityType("PASSPORT");
        request.setIdentityNo("P" + suffix);
        request.setCountry("US");
        request.setState("CA");
        request.setCity("Los Angeles");
        request.setAddress("123 Main St, Apt 4B");
        request.setZipcode("90001");
        return request;
    }

    /**
     * 构造客户更新请求。
     *
     * 该方法只组装更新后的测试客户资料；customerId 由接口路径传入，不放在请求体中。
     *
     * @return 客户更新请求
     */
    static CustomerUpdateRequest updateRequest() {
        String suffix = OrderNoGenerator.generate("CUS_UPD_");
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setFirstname("ABC");
        request.setLastname("Brown");
        request.setEmail("abc_brown_" + suffix + TEST_EMAIL_DOMAIN);
        request.setPhone("13628173753");
        request.setIdentityType("PASSPORT");
        request.setIdentityNo("P" + suffix);
        request.setCountry("US");
        request.setState("NY");
        request.setCity("New York");
        request.setAddress("456 Broadway");
        request.setZipcode("10001");
        return request;
    }

    /**
     * 创建一个可供更新、检索或删除 case 使用的沙盒客户。
     *
     * 该方法会真实调用网关创建客户接口，返回 customerId 供后续目标接口使用；不负责商户本地客户幂等或清理策略。
     *
     * @param client SDK 客户端
     * @return 网关客户 ID
     */
    static String createCustomerForCase(OpenApiClient client) {
        OpenApiResult<CustomerResponse> result = client.createCustomer(createRequest());
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getCustomerId()).isNotBlank();
        return result.getData().getCustomerId();
    }

    /**
     * 构造有序日志字段。
     *
     * 该方法仅用于测试日志输出，不参与签名、加密、客户状态判断或资金处理。
     *
     * @param keyValues key/value 交替传入的字段
     * @return 有序日志 Map
     */
    static Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
