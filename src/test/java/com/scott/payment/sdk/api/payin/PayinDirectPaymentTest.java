package com.scott.payment.sdk.api.payin;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.PaymentMethod;
import com.scott.payment.sdk.model.common.PaymentType;
import com.scott.payment.sdk.model.payment.LocalPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.testkit.RealGatewayTestSupport;
import com.scott.payment.sdk.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinLocalPaymentTest
 * @date : 2026-07-02 11:38
 * @email : scott_x@163.com
 * @description : 本地支付直连代收创建接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并提交 payType=1
 *                和 paymentMethod=CASHAPP 的直连代收参数。本 case 会真实请求 /pay-api/trade/payment，涉及金额、客户资料和
 *                Cash App 支付资料；SDK 只负责请求加密和响应解密，不负责商户本地幂等、支付状态确认、渠道回调或资金对账。
 * @status : create
 */
@Slf4j
public class PayinDirectPaymentTest {

    /**
     * 真实请求测试环境网关创建本地支付直连代收交易。
     *
     * 请求会经过 JWT 签名、请求 data 加密、HTTP 调用、响应 data 解密完整流程；重复运行会生成新的商户订单号。
     */
    @Test
    public void testPayinDirectPayment() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        LocalPaymentRequest request = directPaymentRequest();

        log.info("本地支付直连代收创建真实调用-请求原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
        OpenApiResult<PaymentResponse> result = client.createLocalPayment(request);
        log.info("本地支付直连代收创建真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));
        if (result != null && result.getData() != null) {
            log.info("本地支付直连代收创建真实调用-交易状态映射: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
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
            assertThat(result.getData().getTradeNo()).isNotBlank();
        }
    }

    /**
     * 构建 CASHAPP 本地支付直连代收请求。
     *
     * 本方法只组装请求参数，不发起 HTTP 请求、不执行签名加密、不保存支付资料、不修改资金状态。
     *
     * @return CASHAPP 本地支付直连代收请求
     */
    private LocalPaymentRequest directPaymentRequest() {
        LocalPaymentRequest request = new LocalPaymentRequest();
        request.setOrderNo(OrderNoGenerator.generate("PAYIN_CARD_"));
        request.setPayType(PaymentType.Direct);
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("12.34"));
        request.setNotifyUrl("http://192.168.2.47:58080/payment-sdk/api/webhook/payin");
        request.setClientIp("47.125.221.223");
        request.setWebsite("http://192.168.2.47:5173");
        request.setCustomer(customerInfo());
        request.setMetadata("metadata");

//        request.setPaymentMethod(PaymentMethod.CASHAPP);
//        request.setPaymentMethodData(paymentMethodData(PaymentMethod.CASHAPP));

        request.setPaymentMethod(PaymentMethod.CARD);
        request.setPaymentMethodData(paymentMethodData(PaymentMethod.CARD));
        return request;
    }

    /**
     * 构建客户资料。
     *
     * @return 客户资料
     */
    private CustomerInfo customerInfo() {
        CustomerInfo customer = new CustomerInfo();
        customer.setFirstname("Lily");
        customer.setLastname("Brown");
        customer.setEmail("lily_brown_1782457030419@test.com");
        customer.setPhone("13628173752");
        customer.setCountry("US");
        customer.setState("CA");
        customer.setCity("Los Angeles");
        customer.setAddress("123 Main St, Apt 4B");
        customer.setZipcode("90001");
        return customer;
    }

    /**
     * 构建本地支付直连的支付方式扩展参数。
     *
     * 本方法返回的参数仅用于测试环境；如果切换为 CARD，生产日志不得输出完整卡号和 CVC。
     *
     * @param paymentMethod 支付方式枚举
     * @return 支付方式扩展参数
     */
    private Map<String, Object> paymentMethodData(PaymentMethod paymentMethod) {
        Map<String, Object> paymentMethodData = new HashMap<String, Object>();
        if (PaymentMethod.CASHAPP.equals(paymentMethod)) {
            paymentMethodData.put("cashappAccount", "$123");
            paymentMethodData.put("email", "lily_brown_1782457030419@test.com");
        } else if (PaymentMethod.CARD.equals(paymentMethod)) {
            paymentMethodData.put("number", "5555555555554444");
            paymentMethodData.put("expMonth", "06");
            paymentMethodData.put("expYear", "2029");
            paymentMethodData.put("cvc", "123");
            paymentMethodData.put("email", "lily_brown_1782457030419@test.com");
            paymentMethodData.put("holderName", "Lily Brown");
        } else {
            throw new IllegalArgumentException("当前本地支付示例未配置支付方式扩展参数: " + paymentMethod);
        }
        return paymentMethodData;
    }
}
