package com.scott.payment.sdk.client;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.testkit.CapturingOpenApiTransport;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentCreateTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 创建代收交易接口商户调用 case，演示收银台支付订单号、币种、金额、回跳地址和通知地址的组装方式。
 *                本 case 涉及资金交易创建和 OpenAPI 请求加密；敏感 Header 和卡信息由 SDK 日志工具脱敏，完整明文和密文仅受调试日志开关控制。
 * @status : create
 */
@Slf4j
class PaymentCreateTest {

    /**
     * 验证收银台代收创建接口使用最新 OpenAPI 加密协议。
     */
    @Test
    void createCheckoutPayment_shouldSuccess() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        String merchantOrderNo = OrderNoGenerator.generate("PAY");
        request.setOrderNo(merchantOrderNo);
        request.setCurrency("USD");
        request.setAmount(OpenApiTestSupport.amount("12.34"));
        request.setReturnUrl("https://merchant.example.com/return");
        request.setNotifyUrl("https://merchant.example.com/notify");

        log.info("用例开始: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "PaymentCreateTest",
                "apiName", "Payment Create",
                "request", request,
                "orderNo", request.getOrderNo(),
                "currency", request.getCurrency(),
                "amount", request.getAmount())));
        OpenApiResult<PaymentResponse> result = client.createCheckoutPayment(request);
        log.info("用例结果: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "PaymentCreateTest",
                "apiName", "Payment Create",
                "success", result.isSuccess(),
                "data", result.getData(),
                "requestPath", transport.getLastRequest().getUri().getPath())));

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/payment");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastEnvelope().getLivemode()).isEqualTo(false);
        assertThat(transport.getLastRequest().getBody()).doesNotContain(merchantOrderNo);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }
}
