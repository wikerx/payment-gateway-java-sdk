package com.scott.payment.sdk;

import com.scott.payment.sdk.exception.OpenApiValidationException;
import com.scott.payment.sdk.http.HttpTransport;
import com.scott.payment.sdk.http.SdkHttpRequest;
import com.scott.payment.sdk.http.SdkHttpResponse;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.model.common.ProductInfo;
import com.scott.payment.sdk.model.payment.CardPaymentRequest;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.LocalPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentCreateRequest;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 收银台和直连代收商品参数校验测试。
 *
 * 本测试使用拒绝执行的内存 HTTP 传输层，不访问真实支付网关、不创建交易，只验证必填商品参数会在请求加密和发送前完成校验。
 */
class OpenApiClientPaymentProductValidationTest {

    /**
     * 验证收银台代收缺少商品列表时会在 HTTP 请求前失败。
     */
    @Test
    void createCheckoutPayment_withoutProduct_shouldRejectBeforeTransport() {
        CheckoutPaymentRequest request = baseCheckoutRequest();

        assertThatThrownBy(() -> client().createCheckoutPayment(request))
                .isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("product can not be empty");
    }

    /**
     * 验证直连代收提交空商品列表时会在 HTTP 请求前失败。
     */
    @Test
    void createLocalPayment_withEmptyProduct_shouldRejectBeforeTransport() {
        LocalPaymentRequest request = baseLocalRequest();
        request.setProduct(Collections.<ProductInfo>emptyList());

        assertThatThrownBy(() -> client().createLocalPayment(request))
                .isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("product can not be empty");
    }

    /**
     * 验证商品数组不能包含空元素。
     */
    @Test
    void createCheckoutPayment_withNullProductItem_shouldRejectBeforeTransport() {
        CheckoutPaymentRequest request = baseCheckoutRequest();
        request.setProduct(Collections.singletonList((ProductInfo) null));

        assertThatThrownBy(() -> client().createCheckoutPayment(request))
                .isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("product[0] can not be null");
    }

    /**
     * 验证商品对象的六个文档必填字段都会逐项校验。
     */
    @Test
    void createLocalPayment_withMissingProductFields_shouldRejectEachField() {
        assertMissingField("name");
        assertMissingField("description");
        assertMissingField("sku");
        assertMissingField("quantity");
        assertMissingField("price");
        assertMissingField("url");
    }

    /**
     * 验证收银台和直连代收携带完整商品列表时能够通过本地校验并进入 HTTP 传输层。
     */
    @Test
    void paymentRequests_withCompleteProduct_shouldReachTransport() {
        CheckoutPaymentRequest checkout = baseCheckoutRequest();
        checkout.setProduct(Collections.singletonList(product(null)));
        LocalPaymentRequest local = baseLocalRequest();
        local.setProduct(Collections.singletonList(product(null)));

        assertThatThrownBy(() -> client().createCheckoutPayment(checkout))
                .isInstanceOf(TransportInvokedException.class);
        assertThatThrownBy(() -> client().createLocalPayment(local))
                .isInstanceOf(TransportInvokedException.class);
    }

    /**
     * 验证本次规则只作用于已调整的收银台和本地支付直连 API，不改变独立信用卡直连接口的现有行为。
     */
    @Test
    void createCardPayment_withoutProduct_shouldKeepExistingBehavior() {
        CardPaymentRequest request = new CardPaymentRequest();
        applyBaseFields(request);

        assertThatThrownBy(() -> client().createCardPayment(request))
                .isInstanceOf(TransportInvokedException.class);
    }

    private void assertMissingField(String fieldName) {
        LocalPaymentRequest request = baseLocalRequest();
        request.setProduct(Collections.singletonList(product(fieldName)));

        assertThatThrownBy(() -> client().createLocalPayment(request))
                .isInstanceOf(OpenApiValidationException.class)
                .hasMessageContaining("product[0]." + fieldName);
    }

    private ProductInfo product(String omittedField) {
        ProductInfo product = JsonSupport.fromJson("{"
                + "\"name\":\"SDK Demo Product\","
                + "\"description\":\"Payment Gateway Java SDK test product\","
                + "\"sku\":\"SKU_10001\","
                + "\"quantity\":\"1\","
                + "\"price\":\"12.34\","
                + "\"url\":\"https://merchant.example.com/products/SKU_10001\""
                + "}", ProductInfo.class);
        if ("name".equals(omittedField)) {
            product.setName(null);
        } else if ("description".equals(omittedField)) {
            product.setDescription(null);
        } else if ("sku".equals(omittedField)) {
            product.setSku(null);
        } else if ("quantity".equals(omittedField)) {
            product.setQuantity(null);
        } else if ("price".equals(omittedField)) {
            product.setPrice(null);
        } else if ("url".equals(omittedField)) {
            product.setUrl(null);
        }
        return product;
    }

    private CheckoutPaymentRequest baseCheckoutRequest() {
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        applyBaseFields(request);
        return request;
    }

    private LocalPaymentRequest baseLocalRequest() {
        LocalPaymentRequest request = new LocalPaymentRequest();
        applyBaseFields(request);
        return request;
    }

    private void applyBaseFields(PaymentCreateRequest request) {
        request.setOrderNo("PAYIN_PRODUCT_VALIDATION_10001");
        request.setCurrency("USD");
        request.setAmount(new BigDecimal("12.34"));
    }

    private OpenApiClient client() {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        config.setRawHttpLogEnabled(false);
        return new OpenApiClient(config, new RejectingTransport());
    }

    /**
     * 测试传输层：一旦被调用就抛出标记异常，用于证明请求已经通过 SDK 本地校验。
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
