package com.wikerx.payment.gateway.sdk.client;

import com.wikerx.payment.gateway.sdk.PaymentGatewayClient;
import com.wikerx.payment.gateway.sdk.PaymentGatewayResult;
import com.wikerx.payment.gateway.sdk.model.balance.BalanceResponse;
import com.wikerx.payment.gateway.sdk.model.customer.CustomerCreateRequest;
import com.wikerx.payment.gateway.sdk.model.customer.CustomerResponse;
import com.wikerx.payment.gateway.sdk.model.payment.CheckoutPaymentRequest;
import com.wikerx.payment.gateway.sdk.model.payment.PaymentResponse;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutCancelRequest;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutCancelResponse;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutCreateRequest;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutResponse;
import com.wikerx.payment.gateway.sdk.model.refund.RefundCreateRequest;
import com.wikerx.payment.gateway.sdk.model.refund.RefundResponse;
import com.wikerx.payment.gateway.sdk.testkit.CapturingPaymentGatewayTransport;
import com.wikerx.payment.gateway.sdk.testkit.PaymentGatewayTestSupport;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商户可直接参考的 OpenAPI SDK 调用案例。
 */
class PaymentGatewayMerchantCasesTest {

    /**
     * Case 01：创建收银台代收交易。
     */
    @Test
    void case01CreateCheckoutPayment() {
        PaymentGatewayClient client = caseClient();
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        request.setOrderNo("CASE-PAY-1001");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("12.34"));
        request.setReturnUrl("https://merchant.example.com/return");
        request.setNotifyUrl("https://merchant.example.com/notify");
        request.setPaymentMethod("CHECKOUT");

        PaymentGatewayResult<PaymentResponse> result = client.createCheckoutPayment(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }

    /**
     * Case 02：检索代收交易。
     */
    @Test
    void case02RetrievePayment() {
        PaymentGatewayResult<PaymentResponse> result = caseClient().retrievePayment("pay_123");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isEqualTo("pay_123");
    }

    /**
     * Case 03：创建退款。
     */
    @Test
    void case03CreateRefund() {
        RefundCreateRequest request = new RefundCreateRequest();
        request.setTradeNo("pay_123");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("12.34"));
        request.setRefundAmount(PaymentGatewayTestSupport.amount("12.34"));
        request.setRefundReason("Customer request");

        PaymentGatewayResult<RefundResponse> result = caseClient().createRefund(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }

    /**
     * Case 04：检索退款。
     */
    @Test
    void case04RetrieveRefund() {
        PaymentGatewayResult<RefundResponse> result = caseClient().retrieveRefund("re_123");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    /**
     * Case 05：创建代付交易。
     */
    @Test
    void case05CreatePayout() {
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo("CASE-PO-1001");
        request.setCurrency("USD");
        request.setAmount(PaymentGatewayTestSupport.amount("9.99"));
        request.setClientIp("127.0.0.1");
        request.setPaymentMethod("PAY_PAL");
        Map<String, Object> paymentMethodData = new HashMap<String, Object>();
        paymentMethodData.put("email", "receiver@example.com");
        request.setPaymentMethodData(paymentMethodData);

        PaymentGatewayResult<PayoutResponse> result = caseClient().createPayout(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getTradeNo()).isNotBlank();
    }

    /**
     * Case 06：检索代付交易。
     */
    @Test
    void case06RetrievePayout() {
        PaymentGatewayResult<PayoutResponse> result = caseClient().retrievePayout("po_123");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    /**
     * Case 07：取消代付交易。
     */
    @Test
    void case07CancelPayout() {
        PayoutCancelRequest request = new PayoutCancelRequest();
        request.setTradeNo("po_123");
        request.setRemark("Merchant cancel");

        PaymentGatewayResult<PayoutCancelResponse> result = caseClient().cancelPayout(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    /**
     * Case 08：按币种检索余额。
     */
    @Test
    void case08RetrieveUsdBalance() {
        PaymentGatewayResult<List<BalanceResponse>> result = caseClient().retrieveBalances("USD");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotEmpty();
        assertThat(result.getData().get(0).getCurrency()).isEqualTo("USD");
    }

    /**
     * Case 09：创建客户。
     */
    @Test
    void case09CreateCustomer() {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setFirstname("Ada");
        request.setLastname("Lovelace");
        request.setEmail("ada@example.com");
        request.setPhone("+12025550123");
        request.setCountry("US");

        PaymentGatewayResult<CustomerResponse> result = caseClient().createCustomer(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getCustomerId()).isNotBlank();
    }

    /**
     * Case 10：查询客户。
     */
    @Test
    void case10RetrieveCustomer() {
        PaymentGatewayResult<CustomerResponse> result = caseClient().retrieveCustomer("cus_123");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getCustomerId()).isEqualTo("cus_123");
    }

    private PaymentGatewayClient caseClient() {
        return new PaymentGatewayClient(PaymentGatewayTestSupport.clientConfig(), new CapturingPaymentGatewayTransport());
    }
}
