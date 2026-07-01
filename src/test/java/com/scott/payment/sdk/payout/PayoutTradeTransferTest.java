package com.scott.payment.sdk.payout;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.PaymentMethod;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import com.scott.payment.sdk.model.payout.PayoutResponse;
import com.scott.payment.sdk.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTradeTransferTest
 * @date : 2026-07-01 16:36
 * @email : scott_x@163.com
 * @description : 代付申请接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起 /pay-api/payout/trade/transfer 请求。
 *                本 case 会真实发起 HTTP 请求并可能创建测试代付交易，涉及资金出款申请、客户资料、卡号和 CVC 等敏感数据。
 *                本 case 只用于商户沙盒联调和 SDK 接入参考，不负责生产幂等落库、状态流转、渠道回调或资金最终状态确认。
 * @status : create
 */
@Slf4j
public class PayoutTradeTransferTest {

    /**
     * 真实请求测试环境网关创建代付交易。
     *
     * 该方法使用 SDK 默认配置文件 `merchant-config.properties`，通过 JDK HTTP Transport 发起真实请求。
     * 请求会经过 JWT 签名、请求 data 加密、HTTP 调用、响应 data 解密完整流程；重复运行会生成新的商户订单号。
     * 本方法没有数据库事务，不做商户侧幂等落库，不确认渠道最终出款状态；最终状态应以查询接口或网关回调为准。
     */
    @Test
    public void testPayoutTradeTransfer() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        assumeTrue(isGatewayReachable(config.getBaseUri(), config.getConnectTimeoutMs()),
                "测试网关不可达，已跳过真实代付申请调用: " + config.getBaseUrl());
        OpenApiClient client = new OpenApiClient(config);
        PayoutCreateRequest request = payoutCreateRequest();

        OpenApiResult<PayoutResponse> result = client.createPayout(request);

        log.info("代付申请真实调用-响应原始明文参数: {}", JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTradeNo()).isNotBlank();
        assertThat(result.getLivemode()).isEqualTo(false);
    }

    private PayoutCreateRequest payoutCreateRequest() {
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo(OrderNoGenerator.generate("PAYOUT_"));
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("3.11"));
//        Option
        request.setNotifyUrl("https://manage.forgottenthrone.com/notifyUrl");

        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setCustomer(customerInfo());
//        Option
        request.setMetadata("metadata");

        request.setPaymentMethod(PaymentMethod.CASHAPP);
        request.setPaymentMethodData(cardPaymentMethodData(PaymentMethod.CASHAPP.getCode()));
        return request;
    }

    /**
     * @description : 构建客户信息
     * @author      : scott
     * @date        : 2026/7/1 - 19:04
     * @params      : []
     * @return      : com.scott.payment.sdk.model.common.CustomerInfo
     */
    private CustomerInfo customerInfo() {
        CustomerInfo customer = new CustomerInfo();
        customer.setFirstname("Lily");
        customer.setLastname("Brown");
        customer.setEmail("lily_brown_1782457030419@test.com");
//        Option
        customer.setPhone("13628173752");
        customer.setCountry("US");
        customer.setState("CA");
        customer.setCity("Los Angeles");
        customer.setAddress("123 Main St, Apt 4B");
        customer.setZipcode("90001");
        return customer;
    }

    /**
     * @description : 支付方式对应数据封装
     * @author      : scott
     * @date        : 2026/7/1 - 19:06
     * @params      : [method]
     * @return      : java.util.Map<java.lang.String,java.lang.Object>
     */
    private Map<String, Object> cardPaymentMethodData(String method) {
//        支付方式主体参数映射
        Map<String, Object> paymentMethodData = new HashMap<String, Object>();

        if(PaymentMethod.CASHAPP.getCode().equals(method)) {
            paymentMethodData.put("cashappAccount", "$123");
        } else if (PaymentMethod.CARD.getCode().equals(method)) {
            paymentMethodData.put("number", "4242424242424242");
            paymentMethodData.put("expMonth", "06");
            paymentMethodData.put("expYear", "2026");
            paymentMethodData.put("cvc", "123");
        }

        return paymentMethodData;
    }

    private Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }

    /**
     * 探测测试网关基础地址是否可建立 TCP 连接。
     *
     * 该方法只用于真实联调 case 的前置条件判断，不发送 OpenAPI 业务请求、不执行签名加密、不创建代付交易。
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
            log.warn("代付申请真实调用-测试网关不可达: {}", JsonSupport.toLogJson(logFields(
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
}
