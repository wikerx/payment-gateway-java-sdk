package com.scott.payment.sdk.client;

import com.scott.payment.sdk.PaymentGatewayClient;
import com.scott.payment.sdk.PaymentGatewayResult;
import com.scott.payment.sdk.model.payout.PayoutCancelRequest;
import com.scott.payment.sdk.model.payout.PayoutCancelResponse;
import com.scott.payment.sdk.testkit.CapturingPaymentGatewayTransport;
import com.scott.payment.sdk.testkit.PaymentGatewayTestSupport;
import com.scott.payment.sdk.json.JsonSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTradeTransferCancelTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 代付取消申请接口商户调用 case，负责演示 /pay-api/payout/trade/transfer-cancel 的取消参数和最新 OpenAPI 加密调用。
 *                本 case 涉及代付状态变更但不直接修改资金余额；请求必须加密 tradeNo，幂等与终态保护由网关服务端按交易号处理。
 * @status : create
 */
@Slf4j
class PayoutTradeTransferCancelTest {

    /**
     * 平台代付交易号，来自商户联调示例。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：作为取消代付申请的交易定位字段，必须进入加密请求 data。
     */
    private static final String TRADE_NO = "payout_202606291921574764695";

    /**
     * 验证代付取消申请接口使用 Bearer JWT 与加密 data 提交请求。
     * 该方法不直接修改资金余额，只断言 SDK 请求路径、授权头、livemode 和 tradeNo 明文隔离结果。
     */
    @Test
    void cancelPayoutTradeTransfer() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);
        PayoutCancelRequest request = new PayoutCancelRequest();
        request.setOrderNo("");
        request.setTradeNo(TRADE_NO);
        request.setRemark("取消代付申请");

        log.info("用例开始: {}", JsonSupport.toJson(PaymentGatewayTestSupport.logFields(
                "caseName", "PayoutTradeTransferCancelTest",
                "merchantId", PaymentGatewayTestSupport.merchantId(),
                "tradeNo", request.getTradeNo(),
                "remarkLength", request.getRemark().length())));
        PaymentGatewayResult<PayoutCancelResponse> result = client.cancelPayout(request);
        log.info("用例结果: {}", JsonSupport.toJson(PaymentGatewayTestSupport.logFields(
                "caseName", "PayoutTradeTransferCancelTest",
                "success", result.isSuccess(),
                "requestPath", transport.getLastRequest().getUri().getPath())));

        String requestBody = transport.getLastRequest().getBody();
        assertThat(transport.getLastRequest().getMethod()).isEqualTo("POST");
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/payout/trade/transfer-cancel");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(requestBody).contains("\"livemode\":false", "\"data\"");
        assertThat(requestBody)
                .doesNotContain(TRADE_NO,
                        "\"tradeNo\"",
                        "取消代付申请");
        assertThat(transport.getLastEnvelope()).containsEntry("livemode", PaymentGatewayTestSupport.livemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }
}
