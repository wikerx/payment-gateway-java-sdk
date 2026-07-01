package com.scott.payment.sdk.client;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.PaymentMethod;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import com.scott.payment.sdk.model.payout.PayoutResponse;
import com.scott.payment.sdk.testkit.CapturingOpenApiTransport;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTransferCreateTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 代付申请接口商户调用 case，负责演示 /pay-api/payout/trade/transfer 的请求参数组装和最新 OpenAPI 加密调用。
 *                本 case 涉及资金出款申请、客户资料和卡信息等敏感数据；只验证 SDK 输出密文 data，不负责真实渠道出款或服务端状态流转。
 * @status : create
 */
@Slf4j
class PayoutTransferCreateTest {

    /**
     * 验证代付申请接口使用 Bearer JWT 与加密 data 提交请求。
     * 该方法不发起真实渠道出款，只通过测试 Transport 断言 SDK 请求路径、授权头、livemode 和敏感明文隔离结果。
     */
    @Test
    void createPayoutTransfer_shouldSuccess() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);
        PayoutCreateRequest request = payoutCreateRequest();

        log.info("用例开始: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "PayoutTransferCreateTest",
                "apiName", "Payout Transfer Create",
                "merchantId", OpenApiTestSupport.merchantId(),
                "request", request,
                "cardNumberLength", request.getPaymentMethodData().get("number").toString().length())));
        OpenApiResult<PayoutResponse> result = client.createPayout(request);
        log.info("用例结果: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "PayoutTransferCreateTest",
                "apiName", "Payout Transfer Create",
                "success", result.isSuccess(),
                "requestPath", transport.getLastRequest().getUri().getPath(),
                "data", result.getData())));

        String requestBody = transport.getLastRequest().getBody();
        assertThat(transport.getLastRequest().getMethod()).isEqualTo("POST");
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/payout/trade/transfer");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(requestBody).contains("\"livemode\":false", "\"data\"");
        assertThat(requestBody)
                .doesNotContain(request.getOrderNo(),
                        "4242424242424242",
                        "lily_brown_1782457030419@test.com",
                        "\"cvc\"",
                        "\"customer\"");
        assertThat(transport.getLastEnvelope().getLivemode()).isEqualTo(OpenApiTestSupport.livemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }

    private PayoutCreateRequest payoutCreateRequest() {
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo(OrderNoGenerator.generate("PO"));
        request.setCurrency("USD");
        request.setAmount(OpenApiTestSupport.amount("3.11"));
        request.setNotifyUrl("");
        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setCustomer(customerInfo());
        request.setMetadata("");
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setPaymentMethodData(cardPaymentMethodData());
        return request;
    }

    private CustomerInfo customerInfo() {
        CustomerInfo customer = new CustomerInfo();
        customer.setFirstname("Lily");
        customer.setLastname("Brown");
        customer.setEmail("lily_brown_1782457030419@test.com");
        customer.setPhone("13628173752");
        customer.setCountry("US");
        customer.setState("CA");
        customer.setCity("Los Angeles");
        customer.setAddress("123 Main St, Apt 4B");
        customer.setZipcode("90001");
        return customer;
    }

    private Map<String, Object> cardPaymentMethodData() {
        Map<String, Object> paymentMethodData = new HashMap<String, Object>();
        paymentMethodData.put("number", "4242424242424242");
        paymentMethodData.put("expMonth", "06");
        paymentMethodData.put("expYear", "2026");
        paymentMethodData.put("cvc", "123");
        return paymentMethodData;
    }
}
