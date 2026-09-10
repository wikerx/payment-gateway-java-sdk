package com.scott.payment.sdk.api.payout;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.payout.PayoutCancelRequest;
import com.scott.payment.sdk.model.payout.PayoutCancelResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTradeTransferCancelTest
 * @date : 2026-07-02 11:22
 * @email : scott-***@163.com
 * @description : 代付取消申请接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/payout/trade/transfer-cancel 请求。本 case 会真实请求网关并可能触发代付取消业务校验；
 *                如果交易已成功或已进入不可取消状态，网关可能返回业务失败。本 case 只证明 SDK 已按最新 OpenAPI 加密协议
 *                完成真实调用和响应解密，不负责商户本地幂等、终态保护或资金对账。
 * @status : create
 */
@Slf4j
public class PayoutTradeTransferCancelTest {

    /**
     * 平台代付交易流水号，来自已调通的代付申请响应。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：作为取消代付申请的交易定位字段，进入加密请求 data。
     */
    private static final String tradeNo = "payout_202607021532396969266";

    /**
     * 商户订单号，来自已调通的代付申请响应。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：辅助网关定位商户订单，并用于商户联调核对日志。
     */
    private static final String orderNo = "PAYOUT_20260702153239394000";

    /**
     * 真实请求测试环境网关取消代付交易。
     *
     * 该方法使用 SDK 默认配置文件 `merchant-config.properties`，通过 JDK HTTP Transport 发起真实 POST 请求。
     * 请求会经过 JWT 签名、请求 data 加密、HTTP 调用、响应 data 解密完整流程。
     * 取消已成功或不可取消的交易时，网关可能返回业务失败，因此本 case 不强制断言业务 code 必须成功。
     */
    @Test
    public void testPayoutTradeTransferCancel() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        PayoutCancelRequest request = payoutCancelRequest();

        log.info("代付取消申请真实调用-请求原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
        OpenApiResult<PayoutCancelResponse> result = client.cancelPayout(request);
        log.info("代付取消申请真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
    }

    /**
     * 构建代付取消请求参数。
     *
     * 本方法只组装用于网关取消接口的 tradeNo、orderNo 和 remark，不发起 HTTP 请求、不执行签名加密、不修改资金状态。
     *
     * @return 代付取消请求
     */
    private PayoutCancelRequest payoutCancelRequest() {
        PayoutCancelRequest request = new PayoutCancelRequest();
        request.setTradeNo(tradeNo);
        request.setOrderNo(orderNo);
        request.setRemark("SDK真实调用代付取消申请");
        return request;
    }

    private Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
