package com.scott.payment.sdk.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.model.balance.BalanceResponse;
import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.PaymentType;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import com.scott.payment.sdk.model.customer.CustomerUpdateRequest;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.LocalPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.model.payout.PayoutCancelRequest;
import com.scott.payment.sdk.model.payout.PayoutCancelResponse;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import com.scott.payment.sdk.model.payout.PayoutResponse;
import com.scott.payment.sdk.model.refund.RefundCreateRequest;
import com.scott.payment.sdk.model.refund.RefundResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Invokes existing SDK APIs for the Thymeleaf merchant demo console.
 */
@Service
public class DemoApiService {

    private final OpenApiClientConfig config;

    public DemoApiService() {
        this.config = MerchantConfigLoader.load();
    }

    public OpenApiClientConfig getConfig() {
        return config;
    }

    public DemoApiInvocation invoke(DemoApiDefinition definition, DemoApiForm form) {
        Map<String, String> params = form == null ? Collections.<String, String>emptyMap() : form.getParams();
        Object requestForDisplay = null;
        try {
            OpenApiClient client = new OpenApiClient(config);
            Object result;
            if ("payin-checkout".equals(definition.getCode())) {
                CheckoutPaymentRequest request = checkoutPaymentRequest(params);
                requestForDisplay = request;
                result = client.createCheckoutPayment(request);
            } else if ("payin-direct".equals(definition.getCode())) {
                LocalPaymentRequest request = localPaymentRequest(params);
                requestForDisplay = request;
                result = client.createLocalPayment(request);
            } else if ("payin-retrieve".equals(definition.getCode())) {
                requestForDisplay = oneField("tradeNo", text(params, "tradeNo"));
                result = client.retrievePayment(required(params, "tradeNo"));
            } else if ("refund-create".equals(definition.getCode())) {
                RefundCreateRequest request = refundCreateRequest(params);
                requestForDisplay = request;
                result = client.createRefund(request);
            } else if ("refund-retrieve".equals(definition.getCode())) {
                requestForDisplay = oneField("refundNo", text(params, "refundNo"));
                result = client.retrieveRefund(required(params, "refundNo"));
            } else if ("payout-create".equals(definition.getCode())) {
                PayoutCreateRequest request = payoutCreateRequest(params);
                requestForDisplay = request;
                result = client.createPayout(request);
            } else if ("payout-retrieve".equals(definition.getCode())) {
                requestForDisplay = oneField("tradeNo", text(params, "tradeNo"));
                result = client.retrievePayout(required(params, "tradeNo"));
            } else if ("payout-cancel".equals(definition.getCode())) {
                PayoutCancelRequest request = payoutCancelRequest(params);
                requestForDisplay = request;
                result = client.cancelPayout(request);
            } else if ("balance-retrieve".equals(definition.getCode())) {
                requestForDisplay = oneField("currency", text(params, "currency"));
                result = StringUtils.isBlank(text(params, "currency"))
                        ? client.retrieveBalances()
                        : client.retrieveBalances(text(params, "currency"));
            } else if ("customer-create".equals(definition.getCode())) {
                CustomerCreateRequest request = customerCreateRequest(params);
                requestForDisplay = request;
                result = client.createCustomer(request);
            } else if ("customer-retrieve".equals(definition.getCode())) {
                requestForDisplay = oneField("customerId", text(params, "customerId"));
                result = client.retrieveCustomer(required(params, "customerId"));
            } else if ("customer-update".equals(definition.getCode())) {
                CustomerUpdateRequest request = customerUpdateRequest(params);
                requestForDisplay = request;
                result = client.updateCustomer(required(params, "customerId"), request);
            } else if ("customer-delete".equals(definition.getCode())) {
                requestForDisplay = oneField("customerId", text(params, "customerId"));
                result = client.deleteCustomer(required(params, "customerId"));
            } else if ("customer-list".equals(definition.getCode())) {
                requestForDisplay = oneField("merchantNo", config.getMerchantId());
                result = client.listCustomers();
            } else {
                throw new IllegalArgumentException("Unsupported demo api: " + definition.getCode());
            }
            return DemoApiInvocation.success(summary(result), toPrettyJson(requestForDisplay), toPrettyJson(result));
        } catch (RuntimeException exception) {
            return DemoApiInvocation.failure("调用失败: " + exception.getClass().getSimpleName(),
                    requestForDisplay == null ? toPrettyJson(params) : toPrettyJson(requestForDisplay),
                    exception.getMessage());
        }
    }

    private CheckoutPaymentRequest checkoutPaymentRequest(Map<String, String> params) {
        CheckoutPaymentRequest request = new CheckoutPaymentRequest();
        request.setOrderNo(required(params, "orderNo"));
        request.setCurrency(required(params, "currency"));
        request.setAmount(money(params, "amount"));
        request.setReturnUrl(text(params, "returnUrl"));
        request.setNotifyUrl(text(params, "notifyUrl"));
        request.setClientIp(text(params, "clientIp"));
        request.setWebsite(text(params, "website"));
        request.setMetadata(text(params, "metadata"));
        request.setPaymentMethodTypes(csvSet(params, "paymentMethodTypes"));
        applyPayinCustomer(request, params);
        return request;
    }

    private LocalPaymentRequest localPaymentRequest(Map<String, String> params) {
        LocalPaymentRequest request = new LocalPaymentRequest();
        request.setOrderNo(required(params, "orderNo"));
        request.setPayType(PaymentType.fromCode(integer(params, "payType")));
        request.setCurrency(required(params, "currency"));
        request.setAmount(money(params, "amount"));
        request.setPaymentMethod(required(params, "paymentMethod"));
        request.setPaymentMethodData(jsonMap(params, "paymentMethodData"));
        request.setReturnUrl(text(params, "returnUrl"));
        request.setNotifyUrl(text(params, "notifyUrl"));
        request.setClientIp(text(params, "clientIp"));
        request.setWebsite(text(params, "website"));
        request.setMetadata(text(params, "metadata"));
        applyPayinCustomer(request, params);
        return request;
    }

    private void applyPayinCustomer(com.scott.payment.sdk.model.payment.PaymentCreateRequest request,
                                    Map<String, String> params) {
        if ("customerId".equals(text(params, "customerMode"))) {
            request.setCustomerId(required(params, "customerId"));
            request.setCustomer(null);
            return;
        }
        request.setCustomer(jsonObject(params, "customer", CustomerInfo.class));
        request.setCustomerId(null);
    }

    private RefundCreateRequest refundCreateRequest(Map<String, String> params) {
        RefundCreateRequest request = new RefundCreateRequest();
        request.setTradeNo(required(params, "tradeNo"));
        request.setCurrency(required(params, "currency"));
        request.setAmount(money(params, "amount"));
        request.setRefundAmount(money(params, "refundAmount"));
        request.setRefundReason(required(params, "refundReason"));
        request.setMetadata(text(params, "metadata"));
        request.setRemark(text(params, "remark"));
        return request;
    }

    private PayoutCreateRequest payoutCreateRequest(Map<String, String> params) {
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setOrderNo(required(params, "orderNo"));
        request.setCurrency(required(params, "currency"));
        request.setAmount(money(params, "amount"));
        request.setPaymentMethod(required(params, "paymentMethod"));
        request.setPaymentMethodData(jsonMap(params, "paymentMethodData"));
        request.setNotifyUrl(text(params, "notifyUrl"));
        request.setClientIp(text(params, "clientIp"));
        request.setWebsite(text(params, "website"));
        request.setMetadata(text(params, "metadata"));
        request.setCustomer(jsonObject(params, "customer", CustomerInfo.class));
        return request;
    }

    private PayoutCancelRequest payoutCancelRequest(Map<String, String> params) {
        PayoutCancelRequest request = new PayoutCancelRequest();
        request.setTradeNo(required(params, "tradeNo"));
        request.setOrderNo(required(params, "orderNo"));
        request.setRemark(text(params, "remark"));
        return request;
    }

    private CustomerCreateRequest customerCreateRequest(Map<String, String> params) {
        CustomerCreateRequest request = new CustomerCreateRequest();
        fillCustomerCreate(request, params);
        return request;
    }

    private CustomerUpdateRequest customerUpdateRequest(Map<String, String> params) {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setFirstname(required(params, "firstname"));
        request.setLastname(required(params, "lastname"));
        request.setEmail(required(params, "email"));
        request.setPhone(text(params, "phone"));
        request.setIdentityType(text(params, "identityType"));
        request.setIdentityNo(text(params, "identityNo"));
        request.setCountry(required(params, "country"));
        request.setState(text(params, "state"));
        request.setCity(text(params, "city"));
        request.setAddress(text(params, "address"));
        request.setZipcode(text(params, "zipcode"));
        return request;
    }

    private void fillCustomerCreate(CustomerCreateRequest request, Map<String, String> params) {
        request.setFirstname(required(params, "firstname"));
        request.setLastname(required(params, "lastname"));
        request.setEmail(required(params, "email"));
        request.setPhone(text(params, "phone"));
        request.setIdentityType(text(params, "identityType"));
        request.setIdentityNo(text(params, "identityNo"));
        request.setCountry(required(params, "country"));
        request.setState(text(params, "state"));
        request.setCity(text(params, "city"));
        request.setAddress(text(params, "address"));
        request.setZipcode(text(params, "zipcode"));
    }

    private String summary(Object result) {
        if (result instanceof OpenApiResult) {
            OpenApiResult<?> apiResult = (OpenApiResult<?>) result;
            return apiResult.isSuccess()
                    ? "调用完成，网关业务 code=0"
                    : "调用完成，网关返回业务失败 code=" + apiResult.getCode();
        }
        return "调用完成";
    }

    private String toPrettyJson(Object value) {
        try {
            Object jsonValue = value instanceof String ? JsonSupport.objectMapper().readTree(String.valueOf(value)) : value;
            return JsonSupport.objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonValue);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> jsonMap(Map<String, String> params, String name) {
        String value = required(params, name);
        return JsonSupport.fromJson(value, new TypeReference<Map<String, Object>>() {
        });
    }

    private Set<String> csvSet(Map<String, String> params, String name) {
        String value = text(params, name);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Set<String> values = new LinkedHashSet<String>();
        String[] items = value.split(",");
        for (String item : items) {
            String trimmed = item == null ? "" : item.trim();
            if (StringUtils.isNotBlank(trimmed)) {
                values.add(trimmed);
            }
        }
        return values.isEmpty() ? null : values;
    }

    private <T> T jsonObject(Map<String, String> params, String name, Class<T> type) {
        String value = text(params, name);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return JsonSupport.fromJson(value, type);
    }

    private BigDecimal money(Map<String, String> params, String name) {
        return new BigDecimal(required(params, name));
    }

    private Integer integer(Map<String, String> params, String name) {
        return Integer.valueOf(required(params, name));
    }

    private String required(Map<String, String> params, String name) {
        String value = text(params, name);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Missing required field: " + name);
        }
        return value;
    }

    private String text(Map<String, String> params, String name) {
        String value = params.get(name);
        return value == null ? "" : value.trim();
    }

    private Map<String, String> oneField(String name, String value) {
        return Collections.singletonMap(name, value);
    }
}
