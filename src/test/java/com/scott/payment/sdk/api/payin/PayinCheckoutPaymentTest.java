package com.scott.payment.sdk.api.payin;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.demo.DemoLocalUrls;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.ProductInfo;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.testkit.RealGatewayTestSupport;
import com.scott.payment.sdk.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinCheckoutPaymentTest
 * @date : 2026-07-02 11:38
 * @email : scott-***@163.com
 * @description : 收银台代收创建接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/trade/payment 请求。本 case 会真实创建沙盒代收交易，涉及金额、回跳地址和异步通知地址；
 *                不负责商户本地幂等落库、支付完成确认、回调处理或资金对账。
 * @status : create
 */
@Slf4j
public class PayinCheckoutPaymentTest {

    /**
     * 真实请求测试环境网关创建收银台代收交易。
     *
     * 请求会经过 JWT 签名、请求 data 加密、HTTP 调用、响应 data 解密完整流程；重复运行会生成新的商户订单号。
     */
    @Test
    public void testPayinCheckoutPayment() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        CheckoutPaymentRequest request = checkoutPaymentRequest();

        log.info("收银台代收创建真实调用-请求原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
        OpenApiResult<PaymentResponse> result = client.createCheckoutPayment(request);
        log.info("收银台代收创建真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));
        if (result != null && result.getData() != null) {
            log.info("收银台代收创建真实调用-交易状态映射: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
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
     * 构建收银台代收创建请求。
     *
     * 本方法只组装请求参数，不发起 HTTP 请求、不执行签名加密、不修改资金状态。
     *
     * @return 收银台代收创建请求
     */
    private CheckoutPaymentRequest checkoutPaymentRequest() {
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        request.setOrderNo(OrderNoGenerator.generate("PAYIN_CHECKOUT_"));
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("12.34"));
        request.setProduct(Collections.singletonList(productInfo()));
        request.setReturnUrl(DemoLocalUrls.PAYIN_RETURN_URL);
        request.setNotifyUrl(DemoLocalUrls.PAYIN_NOTIFY_URL);
        request.setCustomer(customerInfo());
        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setMetadata("metadata");
        return request;
    }

    /**
     * 构建符合对外 OpenAPI 必填规则的商品信息。
     *
     * @return 收银台代收测试商品
     */
    private ProductInfo productInfo() {
        ProductInfo product = new ProductInfo();
        product.setName("SDK Demo Product");
        product.setDescription("Payment Gateway Java SDK checkout test product");
        product.setSku("SKU_CHECKOUT_10001");
        product.setQuantity(1);
        product.setPrice("12.34");
        product.setUrl("https://manage.forgottenthrone.com/products/SKU_CHECKOUT_10001");
        return product;
    }

    /**
     * 构建客户资料。
     *
     * 网关当前要求收银台代收请求必须提供 customerId 或 customer。
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
}
