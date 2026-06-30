package com.wikerx.payment.gateway.sdk.client;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClient;
import com.wikerx.payment.gateway.sdk.PaymentGatewayResult;
import com.wikerx.payment.gateway.sdk.logging.PaymentGatewayLogSanitizer;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutResponse;
import com.wikerx.payment.gateway.sdk.testkit.CapturingPaymentGatewayTransport;
import com.wikerx.payment.gateway.sdk.testkit.PaymentGatewayTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTradeTransferRetrieveTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 检索代付交易接口商户调用 case，负责演示 /pay-api/payout/trade/transfer/{tradeNo} 的 Bearer JWT 查询流程。
 *                本 case 只读取代付交易结果，不提交资金变更、不修改状态、不发送请求体；响应 data 由 SDK 按最新 OpenAPI 加密规则解密。
 * @status : create
 */
@Slf4j
class PayoutTradeTransferRetrieveTest {

    /**
     * 平台代付交易号，来自商户联调示例。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与检索代付交易接口的路径拼接和 JWT jti 生成。
     */
    private static final String TRADE_NO = "payout_202606291921574764695";

    /**
     * 验证检索代付交易接口使用 GET、Bearer JWT 且不发送请求体。
     * 该方法不修改资金或交易状态，只断言请求路径、授权头、空请求体和响应解析结果。
     */
    @Test
    void retrievePayoutTradeTransfer() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);

        log.info("检索代付交易 case 开始：merchantId={} tradeNo={}",
                PaymentGatewayLogSanitizer.maskMerchantId(PaymentGatewayTestSupport.merchantId()),
                TRADE_NO);
        PaymentGatewayResult<PayoutResponse> result = client.retrievePayout(TRADE_NO);
        log.info("检索代付交易 case 结果：success={} requestPath={}",
                result.isSuccess(),
                transport.getLastRequest().getUri().getPath());

        assertThat(transport.getLastRequest().getMethod()).isEqualTo("GET");
        assertThat(transport.getLastRequest().getUri().getPath())
                .isEqualTo("/pay-api/payout/trade/transfer/payout_202606291921574764695");
        assertThat(transport.getLastRequest().getBody()).isNull();
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }
}
