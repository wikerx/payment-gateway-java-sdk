package com.scott.payment.sdk.client;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.model.refund.RefundCreateRequest;
import com.scott.payment.sdk.model.refund.RefundResponse;
import com.scott.payment.sdk.testkit.CapturingOpenApiTransport;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
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
 *                本 case 涉及资金反向状态流转和 OpenAPI 请求加密；完整明文和密文仅受调试日志开关控制。
 * @status : create
 */
@Slf4j
class RefundCreateTest {

    /**
     * 验证创建退款接口使用最新 OpenAPI 加密协议。
     */
    @Test
    void createRefund_shouldSuccess() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);
        RefundCreateRequest request = new RefundCreateRequest();
        request.setTradeNo("pay_123");
        request.setCurrency("USD");
        request.setAmount(OpenApiTestSupport.amount("12.34"));
        request.setRefundAmount(OpenApiTestSupport.amount("12.34"));
        request.setRefundReason("Customer request");

        log.info("用例开始: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "RefundCreateTest",
                "apiName", "Refund Create",
                "request", request)));
        OpenApiResult<RefundResponse> result = client.createRefund(request);
        log.info("用例结果: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "RefundCreateTest",
                "apiName", "Refund Create",
                "success", result.isSuccess(),
                "data", result.getData(),
                "requestPath", transport.getLastRequest().getUri().getPath())));

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/refund");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastEnvelope().getLivemode()).isEqualTo(false);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }
}
