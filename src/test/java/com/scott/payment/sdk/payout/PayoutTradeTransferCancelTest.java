package com.scott.payment.sdk.payout;

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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTradeTransferCancelTest
 * @date : 2026-07-02 11:22
 * @email : scott_x@163.com
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
    private static final String TRADE_NO = "payout_202607021132012775210";

    /**
     * 商户订单号，来自已调通的代付申请响应。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：辅助网关定位商户订单，并用于商户联调核对日志。
     */
    private static final String ORDER_NO = "PAYOUT_20260702113201042000";

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
        assumeTrue(isGatewayReachable(config.getBaseUri(), config.getConnectTimeoutMs()),
                "测试网关不可达，已跳过真实代付取消申请调用: " + config.getBaseUrl());
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
        request.setTradeNo(TRADE_NO);
        request.setOrderNo(ORDER_NO);
        request.setRemark("SDK真实调用代付取消申请");
        return request;
    }

    /**
     * 探测测试网关基础地址是否可建立 TCP 连接。
     *
     * 该方法只用于真实联调 case 的前置条件判断，不发送 OpenAPI 业务请求、不执行签名加密、不取消代付交易。
     *
     * @param baseUri merchant-config.properties 中配置的网关基础地址
     * @param connectTimeoutMs 连接超时时间，单位毫秒
     * @return true 表示网关端口可连接，false 表示当前环境不可执行真实 HTTP case
     */
    private boolean isGatewayReachable(URI baseUri, Integer connectTimeoutMs) {
        String host = baseUri.getHost();
        int port = baseUri.getPort();
        if (host == null || port < 0) {
            return false;
        }
        int timeout = connectTimeoutMs == null ? 1000 : Math.min(connectTimeoutMs, 1000);
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException exception) {
            log.warn("代付取消申请真实调用-测试网关不可达: {}", JsonSupport.toLogJson(logFields(
                    "baseUrl", baseUri.toString(),
                    "host", host,
                    "port", port,
                    "message", exception.getMessage())));
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 关闭探测 socket 失败不影响真实联调用例跳过判断。
            }
        }
    }

    private Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
