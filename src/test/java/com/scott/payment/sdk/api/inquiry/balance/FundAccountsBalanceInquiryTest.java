package com.scott.payment.sdk.api.inquiry.balance;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.balance.BalanceResponse;
import com.scott.payment.sdk.testkit.RealGatewayTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : FundAccountsBalanceInquiryRealTest
 * @date : 2026-07-02 12:05
 * @email : scott_x@163.com
 * @description : 检索余额接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/fund/accounts/get?currency=USD 请求。本 case 只读取商户资金账户余额，不修改余额、冻结金额、
 *                提现金额、清结算状态或交易状态；响应 data 由 SDK 按最新 OpenAPI 加密协议解密为余额列表。
 * @status : create
 */
@Slf4j
public class FundAccountsBalanceInquiryTest {

    /**
     * 查询币种，使用 ISO 4217 三位大写代码。
     */
    private static final String CURRENCY = "USD";

    /**
     * 真实请求测试环境网关检索商户余额。
     *
     * GET 请求不发送加密请求体，但仍会携带 Bearer JWT，响应 data 由 SDK 自动解密为 List<BalanceResponse>。
     * 本方法没有数据库事务，不修改资金状态，只用于商户沙盒联调和 SDK 接入参考。
     */
    @Test
    public void testFundAccountsBalanceInquiry() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);

        log.info("检索余额真实调用-请求参数: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
                "currency", CURRENCY,
                "requestPath", "/pay-api/fund/accounts/get?currency=" + CURRENCY)));
        OpenApiResult<List<BalanceResponse>> result = client.retrieveBalances(CURRENCY);
        log.info("检索余额真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().get(0).getMerNo()).isEqualTo(config.getMerchantId());
        assertThat(result.getData().get(0).getCurrency()).isEqualTo(CURRENCY);
    }
}
