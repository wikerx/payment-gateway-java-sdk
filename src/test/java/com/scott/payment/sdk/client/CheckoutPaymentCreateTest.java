package com.scott.payment.sdk.client;

import com.scott.payment.sdk.PaymentGatewayClient;
import com.scott.payment.sdk.PaymentGatewayResult;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.testkit.CapturingPaymentGatewayTransport;
import com.scott.payment.sdk.testkit.PaymentGatewayTestSupport;
import com.scott.payment.sdk.json.JsonSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CheckoutPaymentCreateTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 收银台代收创建接口商户调用 case，演示订单号、币种、金额、回跳地址和通知地址的组装方式。
 *                本 case 涉及资金交易创建和 OpenAPI 请求加密，开启原始日志后会输出完整 JWT、请求明文、密文 data 和解密响应，便于商户核验。
 * @status : create
 */
@Slf4j
class CheckoutPaymentCreateTest {

    /**
     * 验证收银台代收创建接口使用最新 OpenAPI 加密协议。
     */
    @Test
    void createCheckoutPayment() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        request.setOrderNo("CASE-PAY-1001");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("12.34"));
        request.setReturnUrl("https://merchant.example.com/return");
        request.setNotifyUrl("https://merchant.example.com/notify");
        request.setPaymentMethod("CHECKOUT");

        log.info("用例开始: {}", JsonSupport.toJson(PaymentGatewayTestSupport.logFields(
                "caseName", "CheckoutPaymentCreateTest",
                "orderNo", request.getOrderNo(),
                "currency", request.getCurrency(),
                "amount", request.getAmount())));
        PaymentGatewayResult<PaymentResponse> result = client.createCheckoutPayment(request);
        log.info("用例结果: {}", JsonSupport.toJson(PaymentGatewayTestSupport.logFields(
                "caseName", "CheckoutPaymentCreateTest",
                "success", result.isSuccess(),
                "tradeNo", result.getData().getTradeNo(),
                "requestPath", transport.getLastRequest().getUri().getPath())));

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/payment");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastEnvelope()).containsEntry("livemode", false);
        assertThat(transport.getLastRequest().getBody()).doesNotContain("CASE-PAY-1001");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }
}
