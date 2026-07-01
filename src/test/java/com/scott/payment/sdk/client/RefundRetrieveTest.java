package com.scott.payment.sdk.client;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
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
    void retrieveRefund_shouldSuccess() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);

        log.info("用例开始: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "RefundRetrieveTest",
                "apiName", "Refund Retrieve",
                "refundNo", "re_123")));
        OpenApiResult<RefundResponse> result = client.retrieveRefund("re_123");
        log.info("用例结果: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "RefundRetrieveTest",
                "apiName", "Refund Retrieve",
                "success", result.isSuccess(),
                "data", result.getData(),
                "requestPath", transport.getLastRequest().getUri().getPath())));

        assertThat(transport.getLastRequest().getMethod()).isEqualTo("GET");
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/refund/re_123");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }
}
