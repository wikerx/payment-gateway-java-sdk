package com.scott.payment.sdk.client;

import com.scott.payment.sdk.PaymentGatewayClient;
import com.scott.payment.sdk.PaymentGatewayResult;
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
 * @classname : PaymentRetrieveTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 检索代收交易接口商户调用 case，演示按平台代收交易号查询交易结果。
 *                本 case 只读取交易状态，不发起扣款、退款或状态变更；请求使用 Bearer JWT，响应 data 由 SDK 解密。
 * @status : create
 */
@Slf4j
class PaymentRetrieveTest {

    /**
     * 验证检索代收交易接口使用 GET、Bearer JWT 且不发送请求体。
     */
    @Test
    void retrievePayment() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);

        log.info("用例开始: {}", JsonSupport.toJson(PaymentGatewayTestSupport.logFields(
                "caseName", "PaymentRetrieveTest",
                "tradeNo", "pay_123")));
        PaymentGatewayResult<PaymentResponse> result = client.retrievePayment("pay_123");
        log.info("用例结果: {}", JsonSupport.toJson(PaymentGatewayTestSupport.logFields(
                "caseName", "PaymentRetrieveTest",
                "success", result.isSuccess(),
                "tradeNo", result.getData().getTradeNo(),
                "requestPath", transport.getLastRequest().getUri().getPath())));

        assertThat(transport.getLastRequest().getMethod()).isEqualTo("GET");
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/payment/pay_123");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isEqualTo("pay_123");
    }
}
