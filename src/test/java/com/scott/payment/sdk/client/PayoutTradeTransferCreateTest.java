package com.scott.payment.sdk.client;

import com.scott.payment.sdk.PaymentGatewayClient;
import com.scott.payment.sdk.PaymentGatewayResult;
import com.scott.payment.sdk.logging.PaymentGatewayLogSanitizer;
import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import com.scott.payment.sdk.model.payout.PayoutResponse;
import com.scott.payment.sdk.testkit.CapturingPaymentGatewayTransport;
import com.scott.payment.sdk.testkit.PaymentGatewayTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTradeTransferCreateTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 代付申请接口商户调用 case，负责演示 /pay-api/payout/trade/transfer 的请求参数组装和最新 OpenAPI 加密调用。
 *                本 case 涉及资金出款申请、客户资料和卡信息等敏感数据；只验证 SDK 输出密文 data，不负责真实渠道出款或服务端状态流转。
 * @status : create
 */
@Slf4j
class PayoutTradeTransferCreateTest {

    /**
     * 验证代付申请接口使用 Bearer JWT 与加密 data 提交请求。
     * 该方法不发起真实渠道出款，只通过测试 Transport 断言 SDK 请求路径、授权头、livemode 和敏感明文隔离结果。
     */
    @Test
    void createPayoutTradeTransfer() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);
        PayoutCreateRequest request = payoutCreateRequest();

        log.info("代付申请 case 开始：merchantId={} orderNo={} currency={} amount={} paymentMethod={} cardNumberLength={}",
                PaymentGatewayLogSanitizer.maskMerchantId(PaymentGatewayTestSupport.merchantId()),
                request.getOrderNo(),
                request.getCurrency(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getPaymentMethodData().get("number").toString().length());
        PaymentGatewayResult<PayoutResponse> result = client.createPayout(request);
        log.info("代付申请 case 结果：success={} requestPath={} responseTradeNo={}",
                result.isSuccess(),
                transport.getLastRequest().getUri().getPath(),
                result.getData().getTradeNo());

        String requestBody = transport.getLastRequest().getBody();
        assertThat(transport.getLastRequest().getMethod()).isEqualTo("POST");
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/payout/trade/transfer");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(requestBody).contains("\"livemode\":false", "\"data\"");
        assertThat(requestBody)
                .doesNotContain("62497715054059",
                        "4242424242424242",
                        "lily_brown_1782457030419@test.com",
                        "\"cvc\"",
                        "\"customer\"");
        assertThat(transport.getLastEnvelope()).containsEntry("livemode", PaymentGatewayTestSupport.livemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }

    private PayoutCreateRequest payoutCreateRequest() {
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo("62497715054059");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("3.11"));
        request.setNotifyUrl("");
        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setCustomer(customerInfo());
        request.setMetadata("");
        request.setPaymentMethod("CARD");
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
