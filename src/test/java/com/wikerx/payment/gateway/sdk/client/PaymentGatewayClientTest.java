package com.wikerx.payment.gateway.sdk.client;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClient;
import com.wikerx.payment.gateway.sdk.PaymentGatewayResult;
import com.wikerx.payment.gateway.sdk.model.balance.BalanceResponse;
import com.wikerx.payment.gateway.sdk.model.common.CardPaymentMethodData;
import com.wikerx.payment.gateway.sdk.model.payment.CardPaymentRequest;
import com.wikerx.payment.gateway.sdk.model.payment.CheckoutPaymentRequest;
import com.wikerx.payment.gateway.sdk.model.payment.PaymentResponse;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutCreateRequest;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutResponse;
import com.wikerx.payment.gateway.sdk.testkit.CapturingPaymentGatewayTransport;
import com.wikerx.payment.gateway.sdk.testkit.PaymentGatewayTestSupport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentGatewayClientTest {

    /**
     * 验证收银台支付请求只发送加密 data，并能解密响应。
     */
    @Test
    void createCheckoutPaymentShouldSendEncryptedDataOnlyAndDecryptResponse() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);
        CheckoutPaymentRequest request = checkoutRequest();

        PaymentGatewayResult<PaymentResponse> result = client.createCheckoutPayment(request);

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/payment");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Bearer ");
        assertThat(transport.getLastRequest().getBody()).contains("\"data\"");
        assertThat(transport.getLastRequest().getBody()).doesNotContain("ORDER-1001");
        assertThat(transport.getLastPlainBody()).containsEntry("orderNo", "ORDER-1001");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isEqualTo("pay_123");
    }

    /**
     * 验证信用卡请求默认 CARD，并避免卡敏感字段进入 toString。
     */
    @Test
    void createCardPaymentShouldDefaultPaymentMethodToCardAndKeepCardDataOutOfToString() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);
        CardPaymentRequest request = new CardPaymentRequest();
        request.setOrderNo("ORDER-CARD-1");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("20.00"));
        CardPaymentMethodData card = new CardPaymentMethodData();
        card.setNumber("4111111111111111");
        card.setCvc("123");
        request.setPaymentMethodData(card);

        client.createCardPayment(request);

        assertThat(transport.getLastPlainBody()).containsEntry("paymentMethod", "CARD");
        assertThat(card.toString()).doesNotContain("4111111111111111", "123");
    }

    /**
     * 验证当前未标注加密注解的代付接口使用普通 JSON。
     */
    @Test
    void createPayoutShouldUsePlainJsonForCurrentUnannotatedBackendEndpoint() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo("PO-1001");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("9.99"));
        request.setPaymentMethod("PAY_PAL");
        Map<String, Object> methodData = new HashMap<String, Object>();
        methodData.put("email", "receiver@example.com");
        request.setPaymentMethodData(methodData);

        PaymentGatewayResult<PayoutResponse> result = client.createPayout(request);

        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/payout/trade/transfer");
        assertThat(transport.getLastRequest().getBody()).contains("PO-1001");
        assertThat(transport.getLastPlainBody()).containsEntry("orderNo", "PO-1001");
        assertThat(result.getData().getTradeNo()).isEqualTo("pay_123");
    }

    /**
     * 验证支付查询使用 GET 且不发送请求体。
     */
    @Test
    void retrievePaymentShouldUseGetWithoutBody() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);

        client.retrievePayment("pay_123");

        assertThat(transport.getLastRequest().getMethod()).isEqualTo("GET");
        assertThat(transport.getLastRequest().getBody()).isNull();
        assertThat(transport.getLastRequest().getUri().getPath()).isEqualTo("/pay-api/trade/payment/pay_123");
    }

    /**
     * 验证余额查询可解析加密列表响应。
     */
    @Test
    void retrieveBalancesShouldDeserializeEncryptedListData() {
        CapturingPaymentGatewayTransport transport = new CapturingPaymentGatewayTransport();
        PaymentGatewayClient client = new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), transport);

        PaymentGatewayResult<List<BalanceResponse>> result = client.retrieveBalances("USD");

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getCurrency()).isEqualTo("USD");
        assertThat(transport.getLastRequest().getUri().getQuery()).isEqualTo("currency=USD");
        assertThat(transport.getLastRequest().getHeaders().get("Authorization")).startsWith("Payment ");
        assertThat(decodedPaymentAuthorization(transport)).isEqualTo(PaymentGatewayTestSupport.merchantJwtSecret());
    }

    private String decodedPaymentAuthorization(CapturingPaymentGatewayTransport transport) {
        String authorization = transport.getLastRequest().getHeaders().get("Authorization");
        String encodedToken = authorization.substring("Payment ".length());
        return new String(Base64.getDecoder().decode(encodedToken), StandardCharsets.UTF_8);
    }

    private CheckoutPaymentRequest checkoutRequest() {
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        request.setOrderNo("ORDER-1001");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("12.34"));
        request.setReturnUrl("https://merchant.example.com/return");
        request.setNotifyUrl("https://merchant.example.com/notify");
        request.setPaymentMethod("CHECKOUT");
        return request;
    }
}
