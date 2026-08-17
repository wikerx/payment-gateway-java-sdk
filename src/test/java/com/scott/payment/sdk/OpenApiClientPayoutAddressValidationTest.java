package com.scott.payment.sdk;

import com.scott.payment.sdk.exception.OpenApiValidationException;
import com.scott.payment.sdk.http.HttpTransport;
import com.scott.payment.sdk.http.SdkHttpRequest;
import com.scott.payment.sdk.http.SdkHttpResponse;
import com.scott.payment.sdk.model.common.PaymentMethod;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 加密货币代付收款地址校验测试。
 *
 * 本测试使用拒绝执行的内存 HTTP 传输层，不访问真实支付网关、不创建代付交易，仅验证 SDK 发起请求前的参数校验规则。
 */
class OpenApiClientPayoutAddressValidationTest {

    /**
     * 验证 BTC 链上代付和 PYUSD 代付缺少收款地址时，会在发送 HTTP 请求前失败。
     */
    @Test
    void createPayout_cryptoMethodWithoutAddress_shouldRejectBeforeTransport() {
        final OpenApiClient client = client();

        assertThatThrownBy(() -> client.createPayout(request(PaymentMethod.BTC_ON_CHAIN, null)))
                .isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("address can not be blank");

        assertThatThrownBy(() -> client.createPayout(request(PaymentMethod.PYUSD, " ")))
                .isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("address can not be blank");
    }

    /**
     * 验证加密货币代付填写收款地址后能够通过 SDK 本地校验并进入 HTTP 传输层。
     */
    @Test
    void createPayout_cryptoMethodWithAddress_shouldReachTransport() {
        final OpenApiClient client = client();

        assertThatThrownBy(() -> client.createPayout(request(
                PaymentMethod.BTC_ON_CHAIN,
                "bc1qexamplemerchanttestaddress0000000000000")))
                .isInstanceOf(TransportInvokedException.class);
    }

    /**
     * 验证原有支付方式不需要新增的顶层 address 字段，避免本次迭代破坏存量商户请求。
     */
    @Test
    void createPayout_existingMethodWithoutAddress_shouldReachTransport() {
        final OpenApiClient client = client();

        assertThatThrownBy(() -> client.createPayout(request(PaymentMethod.CARD, null)))
                .isInstanceOf(TransportInvokedException.class);
    }

    /**
     * 验证仅支持代收的 BTC_LIGHT_NETWORK 不会被 SDK 作为代付请求发送。
     */
    @Test
    void createPayout_btcLightNetwork_shouldRejectBeforeTransport() {
        final OpenApiClient client = client();

        assertThatThrownBy(() -> client.createPayout(request(PaymentMethod.BTC_LIGHT_NETWORK, null)))
                .isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("BTC_LIGHT_NETWORK only supports payin");
    }

    /**
     * 验证仅支持代收的 GOOGLE_OR_APPLE 不会被 SDK 作为代付请求发送。
     */
    @Test
    void createPayout_googleOrApple_shouldRejectBeforeTransport() {
        final OpenApiClient client = client();

        assertThatThrownBy(() -> client.createPayout(request(PaymentMethod.GOOGLE_OR_APPLE, null)))
                .isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("GOOGLE_OR_APPLE only supports payin");
    }

    private OpenApiClient client() {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        config.setRawHttpLogEnabled(false);
        return new OpenApiClient(config, new RejectingTransport());
    }

    private PayoutCreateRequest request(PaymentMethod paymentMethod, String address) {
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo("PAYOUT_ADDRESS_VALIDATION_10001");
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("13.00"));
        request.setPaymentMethod(paymentMethod);
        request.setPaymentMethodData(new HashMap<String, Object>());
        request.setAddress(address);
        return request;
    }

    /**
     * 测试传输层：一旦被调用就抛出标记异常，用于证明请求已经通过 SDK 本地参数校验。
     */
    private static final class RejectingTransport implements HttpTransport {

        @Override
        public SdkHttpResponse execute(SdkHttpRequest request) {
            throw new TransportInvokedException();
        }
    }

    /**
     * 标记内存传输层已被调用，不代表网关或业务处理失败。
     */
    private static final class TransportInvokedException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }
}
