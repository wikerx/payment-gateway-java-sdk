package com.scott.payment.sdk.api.payout;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.payout.PayoutResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTradeTransferInquiryTest
 * @date : 2026-07-02 11:22
 * @email : scott-***@163.com
 * @description : 检索代付交易接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/payout/trade/transfer/{tradeNo} 请求。本 case 只读取代付交易结果，不提交资金变更、不修改交易状态、
 *                不负责商户本地幂等、终态保护或对账处理；响应 data 由 SDK 按最新 OpenAPI 加密协议解密后输出。
 * @status : create
 */
@Slf4j
public class PayoutTradeTransferInquiryTest {

    /**
     * 平台代付交易流水号，来自已调通的代付申请响应。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：拼接检索代付交易接口路径，并用于商户联调核对响应。
     */
    private static final String tradeNo = "payout_202607021105485695090";

    /**
     * 商户订单号，来自已调通的代付申请响应，仅用于日志核对。
     */
    private static final String orderNo = "PAYOUT_20260702110548372000";

    /**
     * 真实请求测试环境网关检索代付交易。
     *
     * 该方法使用 SDK 默认配置文件 `merchant-config.properties`，通过 JDK HTTP Transport 发起真实 GET 请求。
     * 请求会经过 JWT 签名、HTTP 调用、响应 data 解密完整流程；GET 请求不发送加密请求体，不修改资金或交易状态。
     */
    @Test
    public void testPayoutTradeTransferInquiry() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);

        log.info("检索代付交易真实调用-请求参数: {}", JsonSupport.toLogJson(logFields(
                "tradeNo", tradeNo,
                "orderNo", orderNo,
                "requestPath", "/pay-api/payout/trade/transfer/" + tradeNo)));
        OpenApiResult<PayoutResponse> result = client.retrievePayout(tradeNo);
        log.info("检索代付交易真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));
        if (result != null && result.getData() != null) {
            log.info("检索代付交易真实调用-交易状态映射: {}", JsonSupport.toLogJson(logFields(
                    "status", result.getData().getStatus(),
                    "responseCode", result.getData().getCode(),
                    "statusEnum", result.getData().getStatusEnum().name(),
                    "enumCode", result.getData().getStatusEnum().getCode(),
                    "statusDescription", result.getData().getStatusDescription(),
                    "finalStatus", result.getData().getStatusEnum().isFinalStatus())));
        }

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTradeNo()).isEqualTo(tradeNo);
        assertThat(result.getData().getOrderNo()).isEqualTo(orderNo);
    }

    private Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
