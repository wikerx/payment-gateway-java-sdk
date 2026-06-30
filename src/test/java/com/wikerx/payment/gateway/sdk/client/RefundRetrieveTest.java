package com.wikerx.payment.gateway.sdk.client;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClient;
import com.wikerx.payment.gateway.sdk.PaymentGatewayResult;
import com.wikerx.payment.gateway.sdk.model.refund.RefundResponse;
import com.wikerx.payment.gateway.sdk.testkit.CapturingPaymentGatewayTransport;
import com.wikerx.payment.gateway.sdk.testkit.PaymentGatewayTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : RefundRetrieveTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 检索退款接口商户调用 case，演示按退款标识查询退款申请处理结果。
 *                本 case 只读取退款状态，不修改资金或交易状态；请求使用 Bearer JWT，响应 data 由 SDK 解密。
 * @status : create
 */
@Slf4j
class RefundRetrieveTest {

    /**
     * 验证检索退款接口使用 GET、Bearer JWT 且不发送请求体。
     */
    @Test
    void retrieveRefund() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);

        log.info("检索退款 case 开始：refundNo=re_123");
        PaymentGatewayResult<RefundResponse> result = client.retrieveRefund("re_123");
        log.info("检索退款 case 结果：success={} requestPath={}",
                result.isSuccess(), transport.getLastRequest().getUri().getPath());

        assertThat(transport.getLastRequest().getMethod()).isEqualTo("GET");
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/refund/re_123");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }
}
