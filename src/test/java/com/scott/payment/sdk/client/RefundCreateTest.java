package com.scott.payment.sdk.client;

import com.scott.payment.sdk.PaymentGatewayClient;
import com.scott.payment.sdk.PaymentGatewayResult;
import com.scott.payment.sdk.model.refund.RefundCreateRequest;
import com.scott.payment.sdk.model.refund.RefundResponse;
import com.scott.payment.sdk.testkit.CapturingPaymentGatewayTransport;
import com.scott.payment.sdk.testkit.PaymentGatewayTestSupport;
import com.scott.payment.sdk.json.JsonSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : RefundCreateTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 创建退款接口商户调用 case，演示按原代收交易号、币种、原金额和退款金额提交退款申请。
 *                本 case 涉及资金反向状态流转和 OpenAPI 请求加密，开启原始日志后会输出请求明文、密文 data 和解密响应，便于商户核验。
 * @status : create
 */
@Slf4j
class RefundCreateTest {

    /**
     * 验证创建退款接口使用最新 OpenAPI 加密协议。
     */
    @Test
    void createRefund() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);
        RefundCreateRequest request = new RefundCreateRequest();
        request.setTradeNo("pay_123");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("12.34"));
        request.setRefundAmount(PaymentGatewayTestSupport.amount("12.34"));
        request.setRefundReason("Customer request");

        log.info("用例开始: {}", JsonSupport.toJson(PaymentGatewayTestSupport.logFields(
                "caseName", "RefundCreateTest",
                "tradeNo", request.getTradeNo(),
                "currency", request.getCurrency(),
                "refundAmount", request.getRefundAmount())));
        PaymentGatewayResult<RefundResponse> result = client.createRefund(request);
        log.info("用例结果: {}", JsonSupport.toJson(PaymentGatewayTestSupport.logFields(
                "caseName", "RefundCreateTest",
                "success", result.isSuccess(),
                "tradeNo", result.getData().getTradeNo(),
                "requestPath", transport.getLastRequest().getUri().getPath())));

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/refund");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastEnvelope()).containsEntry("livemode", false);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }
}
