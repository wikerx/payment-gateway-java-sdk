package com.scott.payment.sdk.api.payout;

import com.apifan.common.random.source.InternetSource;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        OpenApiClient client = new OpenApiClient(config);
        PayoutCreateRequest request = payoutCreateRequest();

        OpenApiResult<PayoutResponse> result = client.createPayout(request);

        log.info("代付申请真实调用-响应原始明文参数: {}", JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));
        if (result != null && result.getData() != null) {
            log.info("代付申请真实调用-交易状态映射: {}", JsonSupport.toLogJson(logFields(
                    "status", result.getData().getStatus(),
                    "responseCode", result.getData().getCode(),
                    "responseMessage", result.getData().getMessage(),
                    "statusEnum", result.getData().getStatusEnum().name(),
                    "enumCode", result.getData().getStatusEnum().getCode(),
                    "statusDescription", result.getData().getStatusEnum().getDescription(),
                    "finalStatus", result.getData().getStatusEnum().isFinalStatus())));
        }

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getTradeNo()).isNotBlank();
        assertThat(result.getLivemode()).isEqualTo(false);
    }

    /**
     * @description : 构建请求参数
     * @author      : scott
     * @date        : 2026/7/2 - 09:52
     * @params      : []
     * @return      : com.scott.payment.sdk.model.payout.PayoutCreateRequest
     */
    private PayoutCreateRequest payoutCreateRequest() {
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo(OrderNoGenerator.generate("PAYOUT_"));
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("3.11"));
//        Option
        request.setNotifyUrl("http://192.168.2.47:58080/payment-sdk/api/webhook/payout");

        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setCustomer(customerInfo());
//        Option
        request.setMetadata("metadata");

//        request.setPaymentMethod(PaymentMethod.CASHAPP);
//        request.setPaymentMethodData(cardPaymentMethodData(PaymentMethod.CASHAPP.getCode()));

        request.setPaymentMethod(PaymentMethod.CARD);
        request.setPaymentMethodData(cardPaymentMethodData(PaymentMethod.CARD.getCode()));

//        request.setPaymentMethod(PaymentMethod.PAY_PAL);
//        request.setPaymentMethodData(cardPaymentMethodData(PaymentMethod.PAY_PAL.getCode()));

//        request.setPaymentMethod(PaymentMethod.UPI);
//        request.setPaymentMethodData(cardPaymentMethodData(PaymentMethod.UPI.getCode()));

//        request.setPaymentMethod(PaymentMethod.ACH_DEBIT);
//        request.setPaymentMethodData(cardPaymentMethodData(PaymentMethod.ACH_DEBIT.getCode()));
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
//        不同支付方式
        if(PaymentMethod.CASHAPP.getCode().equals(method)) {
            paymentMethodData.put("cashappAccount", "$123");
        } else if (PaymentMethod.CARD.getCode().equals(method)) {
            paymentMethodData.put("number", "4000056655665556");
            paymentMethodData.put("expMonth", "06");
            paymentMethodData.put("expYear", "2029");
            paymentMethodData.put("cvc", "123");
        }else if (PaymentMethod.PAY_PAL.getCode().equals(method)) {
            paymentMethodData.put("paypalEmail", InternetSource.getInstance().randomEmail(10));
        }else if (PaymentMethod.UPI.getCode().equals(method)) {
            paymentMethodData.put("bankName", "scott's bank");
            paymentMethodData.put("bankCode", "641110");
            paymentMethodData.put("cardNo", "6200000000000005");
        }else if (PaymentMethod.ACH_DEBIT.getCode().equals(method)) {
            paymentMethodData.put("accountNumber", "6205500000000000004");
            paymentMethodData.put("routingNumber", "641110");
        }else{
            log.error("未知支付方式");
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

}
