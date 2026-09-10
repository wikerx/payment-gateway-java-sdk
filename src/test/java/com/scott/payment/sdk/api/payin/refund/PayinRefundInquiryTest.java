package com.scott.payment.sdk.api.payin.refund;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.refund.RefundResponse;
import com.scott.payment.sdk.testkit.RealGatewayTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinRefundInquiryTest
 * @date : 2026-07-02 11:38
 * @email : scott-***@163.com
 * @description : 检索退款接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/trade/refund/{refundNo} 请求。本 case 只读取退款申请处理结果，不创建退款、不修改资金状态；
 *                如果示例退款标识不存在，网关可能返回业务失败或空数据，商户应替换为自己退款申请返回的 charge/refundNo 后联调。
 * @status : create
 */
@Slf4j
public class PayinRefundInquiryTest {

    /**
     * 退款标识符。
     *
     * 商户真实联调时建议替换为退款申请接口返回的 charge/refundNo；当前默认值只用于证明检索退款接口链路可真实请求网关。
     */
    private static final String charge = "charge_202607021549576341310";

    /**
     * 真实请求测试环境网关检索退款申请。
     *
     * GET 请求不发送加密请求体，但仍会携带 Bearer JWT，响应 data 由 SDK 自动解密。
     */
    @Test
    public void testPayinRefundInquiry() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);

        log.info("检索退款真实调用-请求参数: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
                "charge", charge,
                "requestPath", "/pay-api/trade/refund/" + charge)));
        OpenApiResult<RefundResponse> result = client.retrieveRefund(charge);
        log.info("检索退款真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));
        if (result != null && result.getData() != null) {
            log.info("检索退款真实调用-交易状态映射: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
                    "status", result.getData().getStatus(),
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
        }
    }
}
