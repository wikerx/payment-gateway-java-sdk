package com.scott.payment.sdk.payout;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.payout.PayoutResponse;
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
 * @classname : PayoutTradeTransferRetrieveTest
 * @date : 2026-07-02 11:22
 * @email : scott_x@163.com
 * @description : 检索代付交易接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/payout/trade/transfer/{tradeNo} 请求。本 case 只读取代付交易结果，不提交资金变更、不修改交易状态、
 *                不负责商户本地幂等、终态保护或对账处理；响应 data 由 SDK 按最新 OpenAPI 加密协议解密后输出。
 * @status : create
 */
@Slf4j
public class PayoutTradeTransferRetrieveTest {

    /**
     * 平台代付交易流水号，来自已调通的代付申请响应。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：拼接检索代付交易接口路径，并用于商户联调核对响应。
     */
    private static final String TRADE_NO = "payout_202607021105485695090";

    /**
     * 商户订单号，来自已调通的代付申请响应，仅用于日志核对。
     */
    private static final String ORDER_NO = "PAYOUT_20260702110548372000";

    /**
     * 真实请求测试环境网关检索代付交易。
     *
     * 该方法使用 SDK 默认配置文件 `merchant-config.properties`，通过 JDK HTTP Transport 发起真实 GET 请求。
     * 请求会经过 JWT 签名、HTTP 调用、响应 data 解密完整流程；GET 请求不发送加密请求体，不修改资金或交易状态。
     */
    @Test
    public void testPayoutTradeTransferRetrieve() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        assumeTrue(isGatewayReachable(config.getBaseUri(), config.getConnectTimeoutMs()),
                "测试网关不可达，已跳过真实检索代付交易调用: " + config.getBaseUrl());
        OpenApiClient client = new OpenApiClient(config);

        log.info("检索代付交易真实调用-请求参数: {}", JsonSupport.toLogJson(logFields(
                "tradeNo", TRADE_NO,
                "orderNo", ORDER_NO,
                "requestPath", "/pay-api/payout/trade/transfer/" + TRADE_NO)));
        OpenApiResult<PayoutResponse> result = client.retrievePayout(TRADE_NO);
        log.info("检索代付交易真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTradeNo()).isEqualTo(TRADE_NO);
        assertThat(result.getData().getOrderNo()).isEqualTo(ORDER_NO);
    }

    /**
     * 探测测试网关基础地址是否可建立 TCP 连接。
     *
     * 该方法只用于真实联调 case 的前置条件判断，不发送 OpenAPI 业务请求、不执行签名加密、不读取或修改代付交易。
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
            log.warn("检索代付交易真实调用-测试网关不可达: {}", JsonSupport.toLogJson(logFields(
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
