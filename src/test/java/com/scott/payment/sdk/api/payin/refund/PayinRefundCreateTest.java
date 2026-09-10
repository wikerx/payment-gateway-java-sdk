package com.scott.payment.sdk.api.payin.refund;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.ProductInfo;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.model.refund.RefundCreateRequest;
import com.scott.payment.sdk.model.refund.RefundResponse;
import com.scott.payment.sdk.testkit.RealGatewayTestSupport;
import com.scott.payment.sdk.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinRefundCreateTest
 * @date : 2026-07-02 11:38
 * @email : scott-***@163.com
 * @description : 代收退款申请接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/trade/refund 请求。本 case 会真实请求网关并可能触发退款业务校验；
 *                如果原代收交易未支付成功或不可退款，网关可能返回业务失败。本 case 只证明 SDK 已按最新 OpenAPI 加密协议
 *                完成真实调用和响应解密，不负责商户本地退款幂等、状态终态保护或资金对账。
 * @status : create
 */
@Slf4j
public class PayinRefundCreateTest {

    /**
     * 已存在且允许退款的代收交易号。
     *
     * 留空时用例会先创建一笔收银台代收交易作为退款申请参数；如果该交易未完成支付，网关可能拒绝退款。
     */
    private static final String tradeNo = "pay_202607021541448605052";

    /**
     * 真实请求测试环境网关提交代收退款申请。
     *
     * 请求会经过 JWT 签名、请求 data 加密、HTTP 调用、响应 data 解密完整流程。
     * 退款属于资金类请求，本 case 不强制断言业务 code 必须成功，避免把“原交易不可退”等业务拒绝误判为 SDK 失败。
     */
    @Test
    public void testPayinRefundCreate() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        RefundCreateRequest request = refundCreateRequest(client);

        log.info("代收退款申请真实调用-请求原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
        OpenApiResult<RefundResponse> result = client.createRefund(request);
        log.info("代收退款申请真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));
        if (result != null && result.getData() != null) {
            log.info("代收退款申请真实调用-交易状态映射: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
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
            assertThat(result.getData().getTradeNo()).isEqualTo(request.getTradeNo());
        }
    }

    /**
     * 构建退款申请请求。
     *
     * 本方法只组装退款参数；当未配置 tradeNo 时，会先真实创建一笔代收交易用于演示退款参数来源。
     *
     * @param client SDK 客户端
     * @return 退款申请请求
     */
    private RefundCreateRequest refundCreateRequest(OpenApiClient client) {
        String tradeNo = resolvePayinTradeNo(client);
        RefundCreateRequest request = new RefundCreateRequest();
        request.setTradeNo(tradeNo);
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("12.34"));
        request.setRefundAmount(new BigDecimal("1.00"));
        request.setRefundReason("SDK真实调用代收退款申请");
        request.setMetadata("metadata");
        return request;
    }

    /**
     * 解析用于退款的代收交易号。
     *
     * @param client SDK 客户端
     * @return 平台代收交易号
     */
    private String resolvePayinTradeNo(OpenApiClient client) {
        if (StringUtils.isNotBlank(tradeNo)) {
            return tradeNo;
        }
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        request.setOrderNo(OrderNoGenerator.generate("PAYIN_REFUND_"));
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("12.34"));
        request.setProduct(Collections.singletonList(productInfo()));
        request.setReturnUrl("https://manage.forgottenthrone.com/");
        request.setNotifyUrl("http://192.168.2.114:58080/payment-sdk/api/webhook/payin");
        request.setCustomer(customerInfo());
        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setMetadata("metadata");
        OpenApiResult<PaymentResponse> createResult = client.createCheckoutPayment(request);
        log.info("代收退款申请真实调用-前置创建代收响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(createResult)));
        if (createResult != null && createResult.getData() != null) {
            log.info("代收退款申请真实调用-前置代收交易状态映射: {}", JsonSupport.toLogJson(RealGatewayTestSupport.logFields(
                    "status", createResult.getData().getStatus(),
                    "responseCode", createResult.getData().getCode(),
                    "statusEnum", createResult.getData().getStatusEnum().name(),
                    "enumCode", createResult.getData().getStatusEnum().getCode(),
                    "statusDescription", createResult.getData().getStatusDescription(),
                    "finalStatus", createResult.getData().getStatusEnum().isFinalStatus())));
        }
        assertThat(createResult.isSuccess()).isTrue();
        assertThat(createResult.getData()).isNotNull();
        assertThat(createResult.getData().getTradeNo()).isNotBlank();
        return createResult.getData().getTradeNo();
    }

    /**
     * 构建前置收银台代收所需的必填商品信息。
     *
     * @return 退款前置代收测试商品
     */
    private ProductInfo productInfo() {
        ProductInfo product = new ProductInfo();
        product.setName("SDK Refund Test Product");
        product.setDescription("Payment Gateway Java SDK refund prerequisite product");
        product.setSku("SKU_REFUND_10001");
        product.setQuantity(1);
        product.setPrice("12.34");
        product.setUrl("https://manage.forgottenthrone.com/products/SKU_REFUND_10001");
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
