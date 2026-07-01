package com.scott.payment.sdk.client;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.model.balance.BalanceResponse;
import com.scott.payment.sdk.testkit.CapturingOpenApiTransport;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import com.scott.payment.sdk.json.JsonSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : FundAccountsBalanceInquiryTest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 检索余额接口商户调用 case，演示按币种查询 2606177036 沙盒商户资金余额。
 *                本 case 只读取资金数据，不修改余额、冻结金额或结算状态；请求使用 Bearer JWT，响应 data 由 SDK 解密后再断言。
 * @status : create
 */
@Slf4j
class FundAccountsBalanceInquiryTest {

    /**
     * 验证检索余额接口按币种查询并解析加密列表响应。
     */
    @Test
    void fundAccountsBalanceInquiry_shouldSuccess() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);

        log.info("用例开始: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "FundAccountsBalanceInquiryTest",
                "apiName", "Fund Accounts Balance Inquiry",
                "merchantId", OpenApiTestSupport.merchantId(),
                "currency", "USD")));
        OpenApiResult<List<BalanceResponse>> result = client.retrieveBalances("USD");
        log.info("用例结果: {}", JsonSupport.toJson(OpenApiTestSupport.logFields(
                "caseName", "FundAccountsBalanceInquiryTest",
                "apiName", "Fund Accounts Balance Inquiry",
                "success", result.isSuccess(),
                "data", result.getData())));

        assertThat(transport.getLastRequest().getMethod()).isEqualTo("GET");
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/fund/accounts/get");
        assertThat(transport.getLastRequest().getUri().getQuery()).isEqualTo("currency=USD");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().get(0).getCurrency()).isEqualTo("USD");
    }
}
