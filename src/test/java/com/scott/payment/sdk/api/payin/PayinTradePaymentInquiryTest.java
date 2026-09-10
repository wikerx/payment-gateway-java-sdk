package com.scott.payment.sdk.api.payin;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.testkit.RealGatewayTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinTradePaymentInquiryTest
 * @date : 2026-07-02 11:38
 * @email : scott-***@163.com
 * @description : 检索代收交易接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并调用待发布的
 *                /pay-api/trade/payment/{tradeNo} 查询接口。本 case 只读取交易结果，不创建扣款、不退款、不修改交易状态；
 *                如果网关暂未发布该接口，可能返回业务失败或未支持响应，此时应以日志为准排查网关发布状态。
 * @status : create
 */
@Slf4j
public class PayinTradePaymentInquiryTest {

    /**
     * 已存在的代收交易号或文档示例交易号。
     *
     * 检索代收交易接口当前标记为待发布，商户真实联调时应替换为自己创建代收交易后返回的 tradeNo。
     */
    private static final String tradeNo = "pay_202607021541448605052";

    /**
     * 真实请求测试环境网关检索代收交易。
     *
     * GET 请求不发送加密请求体，但仍会携带 Bearer JWT，响应 data 由 SDK 自动解密。
     */
    @Test
    public void testPayinTradePaymentInquiry() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);

        log.info("检索代收交易真实调用-请求参数: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
                "tradeNo", tradeNo,
                "requestPath", "/pay-api/trade/payment/" + tradeNo)));
        OpenApiResult<PaymentResponse> result = client.retrievePayment(tradeNo);
        log.info("检索代收交易真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));
        if (result != null && result.getData() != null) {
            log.info("检索代收交易真实调用-交易状态映射: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
                    "status", result.getData().getStatus(),
                    "responseCode", result.getData().getCode(),
                    "statusEnum", result.getData().getStatusEnum().name(),
                    "enumCode", result.getData().getStatusEnum().getCode(),
                    "statusDescription", result.getData().getStatusDescription(),
                    "finalStatus", result.getData().getStatusEnum().isFinalStatus())));
        }

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        if (result.isSuccess()) {
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getTradeNo()).isEqualTo(tradeNo);
        }
    }
}
