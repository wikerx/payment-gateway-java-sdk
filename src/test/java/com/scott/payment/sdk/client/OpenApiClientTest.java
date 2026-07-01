package com.scott.payment.sdk.client;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.exception.OpenApiResponseException;
import com.scott.payment.sdk.model.balance.BalanceResponse;
import com.scott.payment.sdk.model.common.CardPaymentMethodData;
import com.scott.payment.sdk.model.payment.CardPaymentRequest;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import com.scott.payment.sdk.model.payout.PayoutResponse;
import com.scott.payment.sdk.testkit.CapturingOpenApiTransport;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiClientTest
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI 客户端单元测试，负责验证代收、代付、余额查询等 SDK 方法的 Bearer JWT、加密请求体、GET 空 body 和响应解密行为。
 *                本测试使用内存 Transport 模拟网关，不发起真实 HTTP 请求、不新增数据库数据、不修改资金或交易状态。
 * @status : create
 */
class OpenApiClientTest {

    /**
     * 验证收银台支付请求只发送加密 data，并能解密响应。
     */
    @Test
    void createCheckoutPayment_shouldSuccessShouldSendEncryptedDataOnlyAndDecryptResponse() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);
        CheckoutPaymentRequest request = checkoutRequest();

        OpenApiResult<PaymentResponse> result = client.createCheckoutPayment(request);

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/payment");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastRequest().getBody()).contains("\"data\"");
        assertThat(transport.getLastEnvelope().getLivemode()).isEqualTo(OpenApiTestSupport.livemode());
        assertThat(transport.getLastRequest().getBody()).doesNotContain("ORDER-1001");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isEqualTo("pay_123");
    }

    /**
     * 验证信用卡请求默认 CARD，并避免卡敏感字段进入 toString。
     */
    @Test
    void createCardPaymentShouldDefaultPaymentMethodToCardAndKeepCardDataOutOfToString() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);
        CardPaymentRequest request = new CardPaymentRequest();
        request.setOrderNo("ORDER-CARD-1");
        request.setCurrency("USD");
        request.setAmount(OpenApiTestSupport.amount("20.00"));
        CardPaymentMethodData card = new CardPaymentMethodData();
        card.setNumber("4111111111111111");
        card.setCvc("123");
        request.setPaymentMethodData(card);

        client.createCardPayment(request);

        assertThat(transport.getLastRequest().getBody()).doesNotContain("4111111111111111", "123");
        assertThat(card.toString()).doesNotContain("4111111111111111", "123");
    }

    /**
     * 验证代付创建按新协议发送加密 data。
     */
    @Test
    void createPayoutShouldSendEncryptedData() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo("PO-1001");
        request.setCurrency("USD");
        request.setAmount(OpenApiTestSupport.amount("9.99"));
        request.setPaymentMethod("PAY_PAL");
        Map<String, Object> methodData = new HashMap<String, Object>();
        methodData.put("email", "receiver@example.com");
        request.setPaymentMethodData(methodData);

        OpenApiResult<PayoutResponse> result = client.createPayout(request);

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/payout/trade/transfer");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastRequest().getBody()).contains("\"data\"");
        assertThat(transport.getLastEnvelope().getLivemode()).isEqualTo(OpenApiTestSupport.livemode());
        assertThat(transport.getLastRequest().getBody()).doesNotContain("PO-1001");
        assertThat(result.getData().getTradeNo()).isEqualTo("pay_123");
    }

    /**
     * 验证支付查询使用 GET 且不发送请求体。
     */
    @Test
    void retrievePaymentShouldUseGetWithoutBody() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);

        client.retrievePayment("pay_123");

        assertThat(transport.getLastRequest().getMethod()).isEqualTo("GET");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastRequest().getBody()).isNull();
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/payment/pay_123");
    }

    /**
     * 验证余额查询可解析加密列表响应。
     */
    @Test
    void retrieveBalancesShouldDeserializeEncryptedListData() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);

        OpenApiResult<List<BalanceResponse>> result = client.retrieveBalances("USD");

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getCurrency()).isEqualTo("USD");
        assertThat(transport.getLastRequest().getUri().getQuery()).isEqualTo("currency=USD");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
    }

    /**
     * 验证 SDK 会拒绝与本地配置不一致的响应环境。
     */
    @Test
    void retrieveBalancesShouldRejectMismatchedResponseLivemode() {
        CapturingOpenApiTransport transport = new CapturingOpenApiTransport();
        transport.setResponseLivemode(!OpenApiTestSupport.livemode());
        OpenApiClient client = new OpenApiClient(OpenApiTestSupport.clientConfig(), transport);

        assertThatThrownBy(() -> client.retrieveBalances("USD"))
                .isInstanceOf(OpenApiResponseException.class)
                .hasMessageContaining("livemode is inconsistent");
    }

    private CheckoutPaymentRequest checkoutRequest() {
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        request.setOrderNo("ORDER-1001");
        request.setCurrency("USD");
        request.setAmount(OpenApiTestSupport.amount("12.34"));
        request.setReturnUrl("https://merchant.example.com/return");
        request.setNotifyUrl("https://merchant.example.com/notify");
        request.setPaymentMethod("CHECKOUT");
        return request;
    }
}
