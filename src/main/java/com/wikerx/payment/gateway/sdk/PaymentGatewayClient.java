package com.wikerx.payment.gateway.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.wikerx.payment.gateway.sdk.config.MerchantConfigLoader;
import com.wikerx.payment.gateway.sdk.config.PaymentGatewayConstants;
import com.wikerx.payment.gateway.sdk.crypto.OpenApiPayloadCrypto;
import com.wikerx.payment.gateway.sdk.crypto.RsaKeyUtils;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayHttpException;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayResponseException;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayValidationException;
import com.wikerx.payment.gateway.sdk.http.HttpTransport;
import com.wikerx.payment.gateway.sdk.http.Jdk8HttpTransport;
import com.wikerx.payment.gateway.sdk.http.SdkHttpRequest;
import com.wikerx.payment.gateway.sdk.http.SdkHttpResponse;
import com.wikerx.payment.gateway.sdk.json.JsonSupport;
import com.wikerx.payment.gateway.sdk.jwt.MerchantJwtSigner;
import com.wikerx.payment.gateway.sdk.logging.PaymentGatewayLogSanitizer;
import com.wikerx.payment.gateway.sdk.model.balance.BalanceResponse;
import com.wikerx.payment.gateway.sdk.model.common.EncryptedRequest;
import com.wikerx.payment.gateway.sdk.model.customer.CustomerCreateRequest;
import com.wikerx.payment.gateway.sdk.model.customer.CustomerResponse;
import com.wikerx.payment.gateway.sdk.model.payment.CardPaymentRequest;
import com.wikerx.payment.gateway.sdk.model.payment.CheckoutPaymentRequest;
import com.wikerx.payment.gateway.sdk.model.payment.LocalPaymentRequest;
import com.wikerx.payment.gateway.sdk.model.payment.PaymentCreateRequest;
import com.wikerx.payment.gateway.sdk.model.payment.PaymentResponse;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutCancelRequest;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutCancelResponse;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutCreateRequest;
import com.wikerx.payment.gateway.sdk.model.payout.PayoutResponse;
import com.wikerx.payment.gateway.sdk.model.refund.RefundCreateRequest;
import com.wikerx.payment.gateway.sdk.model.refund.RefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentGatewayClient
 * @date : 2026-06-30 10:28
 * @email : <git账户邮箱>
 * @description : 商户支付网关 Java SDK 客户端，负责请求签名、请求加密、响应解密、HTTP 调用和基础参数校验。
 * @status : create
 */
@Slf4j
public class PaymentGatewayClient {

    /**
     * 后端当前并存的新旧鉴权协议。
     */
    private enum AuthorizationMode {
        /**
         * 标注 @VerificationAndProcessing 的接口使用 Bearer JWT。
         */
        BEARER_JWT,
        /**
         * 未标注新协议的接口仍使用 Payment Base64(apiPrivateKey)。
         */
        PAYMENT_KEY
    }

    /**
     * SDK 客户端运行配置。
     */
    private final PaymentGatewayClientConfig config;
    /**
     * HTTP 传输实现。
     */
    private final HttpTransport httpTransport;
    /**
     * OpenAPI payload 加解密组件。
     */
    private final OpenApiPayloadCrypto payloadCrypto;
    /**
     * 商户 JWT 签名组件。
     */
    private final MerchantJwtSigner jwtSigner;
    /**
     * 平台请求公钥。
     */
    private final PublicKey platformPublicKey;
    /**
     * 商户响应私钥。
     */
    private final PrivateKey merchantResponsePrivateKey;

    /**
     * 使用默认 JDK HTTP 传输层创建客户端。
     *
     * @param config SDK 客户端配置
     */
    public PaymentGatewayClient(PaymentGatewayClientConfig config) {
        this(config, new Jdk8HttpTransport(), new OpenApiPayloadCrypto(), new MerchantJwtSigner());
    }

    /**
     * 从默认 classpath 配置文件创建客户端。
     *
     * @return SDK 客户端
     */
    public static PaymentGatewayClient create() {
        return new PaymentGatewayClient(MerchantConfigLoader.load());
    }

    /**
     * 从指定 classpath 配置文件创建客户端。
     *
     * @param configFileName 配置文件名
     * @return SDK 客户端
     */
    public static PaymentGatewayClient create(String configFileName) {
        return new PaymentGatewayClient(MerchantConfigLoader.load(configFileName));
    }

    /**
     * 使用自定义 HTTP 传输层创建客户端。
     *
     * @param config SDK 客户端配置
     * @param httpTransport HTTP 传输实现
     */
    public PaymentGatewayClient(PaymentGatewayClientConfig config, HttpTransport httpTransport) {
        this(config, httpTransport, new OpenApiPayloadCrypto(), new MerchantJwtSigner());
    }

    /**
     * 使用自定义核心组件创建客户端，主要用于测试或商户高级扩展。
     *
     * @param config SDK 客户端配置
     * @param httpTransport HTTP 传输实现
     * @param payloadCrypto payload 加解密组件
     * @param jwtSigner JWT 签名组件
     */
    public PaymentGatewayClient(PaymentGatewayClientConfig config,
                                HttpTransport httpTransport,
                                OpenApiPayloadCrypto payloadCrypto,
                                MerchantJwtSigner jwtSigner) {
        this.config = Objects.requireNonNull(config, "config can not be null");
        this.httpTransport = Objects.requireNonNull(httpTransport, "httpTransport can not be null");
        this.payloadCrypto = Objects.requireNonNull(payloadCrypto, "payloadCrypto can not be null");
        this.jwtSigner = Objects.requireNonNull(jwtSigner, "jwtSigner can not be null");
        this.config.validate();
        this.platformPublicKey = RsaKeyUtils.readPublicKey(config.getPlatformPublicKey());
        this.merchantResponsePrivateKey = RsaKeyUtils.readPrivateKey(config.getMerchantResponsePrivateKey());
    }

    /**
     * 创建收银台代收交易。
     *
     * @param request 收银台支付请求
     * @return 代收交易响应
     */
    public PaymentGatewayResult<PaymentResponse> createCheckoutPayment(CheckoutPaymentRequest request) {
        return createPayment(request);
    }

    /**
     * 创建本地支付直连交易。
     *
     * @param request 本地支付请求
     * @return 代收交易响应
     */
    public PaymentGatewayResult<PaymentResponse> createLocalPayment(LocalPaymentRequest request) {
        return createPayment(request);
    }

    /**
     * 创建信用卡直连交易。
     *
     * @param request 信用卡支付请求
     * @return 代收交易响应
     */
    public PaymentGatewayResult<PaymentResponse> createCardPayment(CardPaymentRequest request) {
        if (request != null && StringUtils.isBlank(request.getPaymentMethod())) {
            request.setPaymentMethod("CARD");
        }
        return createPayment(request);
    }

    /**
     * 查询代收交易。
     *
     * @param tradeNo 平台交易流水号
     * @return 代收交易响应
     */
    public PaymentGatewayResult<PaymentResponse> retrievePayment(String tradeNo) {
        return get(String.format(PaymentGatewayConstants.PAYMENT_RETRIEVE_PATH, encodePath(requireText(tradeNo, "tradeNo"))),
                PaymentResponse.class,
                "query-" + UUID.randomUUID());
    }

    /**
     * 创建代付交易。
     *
     * @param request 代付创建请求
     * @return 代付交易响应
     */
    public PaymentGatewayResult<PayoutResponse> createPayout(PayoutCreateRequest request) {
        validatePayoutCreateRequest(request);
        return postJson(PaymentGatewayConstants.PAYOUT_CREATE_PATH,
                request,
                PayoutResponse.class,
                requireText(request.getOrderNo(), "orderNo"));
    }

    /**
     * 查询代付交易。
     *
     * @param tradeNo 平台代付流水号
     * @return 代付交易响应
     */
    public PaymentGatewayResult<PayoutResponse> retrievePayout(String tradeNo) {
        return get(String.format(PaymentGatewayConstants.PAYOUT_RETRIEVE_PATH, encodePath(requireText(tradeNo, "tradeNo"))),
                PayoutResponse.class,
                "query-" + UUID.randomUUID());
    }

    /**
     * 取消代付交易。
     *
     * @param request 代付取消请求
     * @return 代付取消响应
     */
    public PaymentGatewayResult<PayoutCancelResponse> cancelPayout(PayoutCancelRequest request) {
        requireObject(request, "request");
        String jti = StringUtils.defaultIfBlank(request.getTradeNo(), request.getOrderNo());
        return postJson(PaymentGatewayConstants.PAYOUT_CANCEL_PATH,
                request,
                PayoutCancelResponse.class,
                requireText(jti, "tradeNo or orderNo"));
    }

    /**
     * 创建退款。
     *
     * @param request 退款创建请求
     * @return 退款响应
     */
    public PaymentGatewayResult<RefundResponse> createRefund(RefundCreateRequest request) {
        validateRefundCreateRequest(request);
        String jti = StringUtils.defaultIfBlank(request.getCharge(),
                request.getTradeNo() + "-" + request.getRefundAmount());
        return postJson(PaymentGatewayConstants.REFUND_CREATE_PATH, request, RefundResponse.class, jti);
    }

    /**
     * 查询退款。
     *
     * @param refundNo 退款标识符
     * @return 退款响应
     */
    public PaymentGatewayResult<RefundResponse> retrieveRefund(String refundNo) {
        return get(String.format(PaymentGatewayConstants.REFUND_RETRIEVE_PATH, encodePath(requireText(refundNo, "refundNo"))),
                RefundResponse.class,
                "query-" + UUID.randomUUID());
    }

    /**
     * 查询商户全部余额。
     *
     * @return 余额响应列表
     */
    public PaymentGatewayResult<List<BalanceResponse>> retrieveBalances() {
        return getList(PaymentGatewayConstants.BALANCE_RETRIEVE_PATH,
                BalanceResponse.class,
                "balance-" + UUID.randomUUID());
    }

    /**
     * 按币种查询商户余额。
     *
     * @param currency 币种
     * @return 余额响应列表
     */
    public PaymentGatewayResult<List<BalanceResponse>> retrieveBalances(String currency) {
        String path = PaymentGatewayConstants.BALANCE_RETRIEVE_PATH + "?currency=" + encodeQuery(requireText(currency, "currency"));
        return getList(path, BalanceResponse.class, "balance-" + UUID.randomUUID());
    }

    /**
     * 创建客户。
     *
     * @param request 客户创建请求
     * @return 客户响应
     */
    public PaymentGatewayResult<CustomerResponse> createCustomer(CustomerCreateRequest request) {
        validateCustomerCreateRequest(request);
        String jti = "customer-" + StringUtils.defaultIfBlank(request.getEmail(), UUID.randomUUID().toString());
        return postJson(PaymentGatewayConstants.CUSTOMER_CREATE_PATH, request, CustomerResponse.class, jti);
    }

    /**
     * 查询客户。
     *
     * @param customerId 客户 ID
     * @return 客户响应
     */
    public PaymentGatewayResult<CustomerResponse> retrieveCustomer(String customerId) {
        return get(String.format(PaymentGatewayConstants.CUSTOMER_RETRIEVE_PATH, encodePath(requireText(customerId, "customerId"))),
                CustomerResponse.class,
                "query-" + UUID.randomUUID());
    }

    /**
     * 发送加密 POST 请求。
     *
     * @param path 接口路径
     * @param request 明文请求对象
     * @param responseType 响应 data 类型
     * @param jwtId JWT jti
     * @param <T> 响应 data 类型
     * @return SDK 响应
     */
    public <T> PaymentGatewayResult<T> postEncrypted(String path, Object request, Class<T> responseType, String jwtId) {
        requireObject(request, "request");
        String requestJson = JsonSupport.toJson(request);
        String encryptedData = payloadCrypto.encrypt(requestJson, platformPublicKey);
        return execute("POST", path, JsonSupport.toJson(new EncryptedRequest(encryptedData)), responseType, jwtId,
                AuthorizationMode.BEARER_JWT);
    }

    /**
     * 发送普通 JSON POST 请求。
     *
     * @param path 接口路径
     * @param request 请求对象
     * @param responseType 响应 data 类型
     * @param jwtId JWT jti
     * @param <T> 响应 data 类型
     * @return SDK 响应
     */
    public <T> PaymentGatewayResult<T> postJson(String path, Object request, Class<T> responseType, String jwtId) {
        requireObject(request, "request");
        return execute("POST", path, JsonSupport.toJson(request), responseType, jwtId, AuthorizationMode.PAYMENT_KEY);
    }

    /**
     * 发送 GET 请求。
     *
     * @param path 接口路径
     * @param responseType 响应 data 类型
     * @param jwtId JWT jti
     * @param <T> 响应 data 类型
     * @return SDK 响应
     */
    public <T> PaymentGatewayResult<T> get(String path, Class<T> responseType, String jwtId) {
        return execute("GET", path, null, responseType, jwtId, AuthorizationMode.PAYMENT_KEY);
    }

    /**
     * 发送 GET 请求并解析列表 data。
     *
     * @param path 接口路径
     * @param elementType 列表元素类型
     * @param jwtId JWT jti
     * @param <T> 列表元素类型
     * @return SDK 列表响应
     */
    public <T> PaymentGatewayResult<List<T>> getList(String path, Class<T> elementType, String jwtId) {
        PaymentGatewayResult<JsonNode> rawResult = executeRaw("GET", path, null, jwtId, AuthorizationMode.PAYMENT_KEY);
        return convertListResult(rawResult, elementType);
    }

    private PaymentGatewayResult<PaymentResponse> createPayment(PaymentCreateRequest request) {
        validatePaymentCreateRequest(request);
        return postEncrypted(PaymentGatewayConstants.PAYMENT_CREATE_PATH,
                request,
                PaymentResponse.class,
                requireText(request.getOrderNo(), "orderNo"));
    }

    private <T> PaymentGatewayResult<T> execute(String method,
                                                String path,
                                                String body,
                                                Class<T> responseType,
                                                String jwtId,
                                                AuthorizationMode authorizationMode) {
        PaymentGatewayResult<JsonNode> rawResult = executeRaw(method, path, body, jwtId, authorizationMode);
        return convertResult(rawResult, responseType);
    }

    private PaymentGatewayResult<JsonNode> executeRaw(String method,
                                                      String path,
                                                      String body,
                                                      String jwtId,
                                                      AuthorizationMode authorizationMode) {
        String requestId = UUID.randomUUID().toString();
        long startMillis = System.currentTimeMillis();
        log.info("event=payment_gateway_sdk_request_start method={} path={} merchantId={} requestId={}",
                method,
                path,
                PaymentGatewayLogSanitizer.maskMerchantId(config.getMerchantId()),
                requestId);
        try {
            SdkHttpResponse response = httpTransport.execute(SdkHttpRequest.builder()
                    .method(method)
                    .uri(resolveUri(path))
                    .headers(headers(jwtId, requestId, body != null, authorizationMode))
                    .body(body)
                    .connectTimeoutMs(config.getConnectTimeoutMs())
                    .readTimeoutMs(config.getReadTimeoutMs())
                    .build());
            if (response.getStatusCode() < PaymentGatewayConstants.HTTP_STATUS_SUCCESS_MIN
                    || response.getStatusCode() >= PaymentGatewayConstants.HTTP_STATUS_SUCCESS_MAX_EXCLUSIVE) {
                log.warn("event=payment_gateway_sdk_request_end method={} path={} merchantId={} requestId={} statusCode={} elapsedMillis={} success=false",
                        method,
                        path,
                        PaymentGatewayLogSanitizer.maskMerchantId(config.getMerchantId()),
                        requestId,
                        response.getStatusCode(),
                        System.currentTimeMillis() - startMillis);
                throw new PaymentGatewayHttpException("Payment Gateway HTTP status is not successful: " + response.getStatusCode());
            }
            log.info("event=payment_gateway_sdk_request_end method={} path={} merchantId={} requestId={} statusCode={} elapsedMillis={} success=true",
                    method,
                    path,
                    PaymentGatewayLogSanitizer.maskMerchantId(config.getMerchantId()),
                    requestId,
                    response.getStatusCode(),
                    System.currentTimeMillis() - startMillis);
            return JsonSupport.fromJson(response.getBody(), new TypeReference<PaymentGatewayResult<JsonNode>>() {
            });
        } catch (RuntimeException exception) {
            log.warn("event=payment_gateway_sdk_request_error method={} path={} merchantId={} requestId={} elapsedMillis={} errorType={} message=\"Payment Gateway request failed\"",
                    method,
                    path,
                    PaymentGatewayLogSanitizer.maskMerchantId(config.getMerchantId()),
                    requestId,
                    System.currentTimeMillis() - startMillis,
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private <T> PaymentGatewayResult<T> convertResult(PaymentGatewayResult<JsonNode> rawResult, Class<T> responseType) {
        PaymentGatewayResult<T> result = new PaymentGatewayResult<T>();
        result.setCode(rawResult.getCode());
        result.setMsg(rawResult.getMsg());
        result.setLivemode(rawResult.getLivemode());
        if (rawResult.getData() != null && !rawResult.getData().isNull()) {
            String plainJson = resolveDataJson(rawResult.getData());
            result.setData(JsonSupport.fromJson(plainJson, responseType));
        }
        return result;
    }

    private <T> PaymentGatewayResult<List<T>> convertListResult(PaymentGatewayResult<JsonNode> rawResult, Class<T> elementType) {
        PaymentGatewayResult<List<T>> result = new PaymentGatewayResult<List<T>>();
        result.setCode(rawResult.getCode());
        result.setMsg(rawResult.getMsg());
        result.setLivemode(rawResult.getLivemode());
        if (rawResult.getData() != null && !rawResult.getData().isNull()) {
            String plainJson = resolveDataJson(rawResult.getData());
            JsonNode plainNode = JsonSupport.fromJson(plainJson, JsonNode.class);
            if (plainNode.isArray()) {
                result.setData(JsonSupport.fromJsonList(plainJson, elementType));
            } else {
                result.setData(java.util.Collections.singletonList(JsonSupport.fromJson(plainJson, elementType)));
            }
        }
        return result;
    }

    private String resolveDataJson(JsonNode dataNode) {
        if (dataNode.isTextual()) {
            return payloadCrypto.decrypt(dataNode.asText(), merchantResponsePrivateKey);
        }
        try {
            return JsonSupport.objectMapper().writeValueAsString(dataNode);
        } catch (JsonProcessingException exception) {
            throw new PaymentGatewayResponseException("Payment Gateway response data can not be serialized", exception);
        }
    }

    private Map<String, String> headers(String jwtId, String requestId, boolean withBody, AuthorizationMode authorizationMode) {
        Map<String, String> headers = new HashMap<String, String>(PaymentGatewayConstants.HTTP_HEADER_MAP_SIZE);
        headers.put(PaymentGatewayConstants.HEADER_AUTHORIZATION, authorizationValue(jwtId, authorizationMode));
        headers.put(PaymentGatewayConstants.HEADER_ACCEPT, PaymentGatewayConstants.ACCEPT);
        headers.put(PaymentGatewayConstants.HEADER_USER_AGENT, PaymentGatewayConstants.USER_AGENT);
        headers.put(PaymentGatewayConstants.HEADER_REQUEST_ID, requestId);
        if (withBody) {
            headers.put(PaymentGatewayConstants.HEADER_CONTENT_TYPE, PaymentGatewayConstants.CONTENT_TYPE);
        }
        return headers;
    }

    private String authorizationValue(String jwtId, AuthorizationMode authorizationMode) {
        if (AuthorizationMode.PAYMENT_KEY.equals(authorizationMode)) {
            String encodedApiPrivateKey = Base64.getEncoder()
                    .encodeToString(config.getMerchantJwtSecret().getBytes(StandardCharsets.UTF_8));
            return PaymentGatewayConstants.AUTHORIZATION_PAYMENT_PREFIX + encodedApiPrivateKey;
        }
        Date now = new Date(config.getClock().millis());
        String token = jwtSigner.sign(
                config.getMerchantId(),
                config.getMerchantJwtSecret(),
                requireText(jwtId, "jwtId"),
                now,
                config.getJwtTtlSeconds());
        return PaymentGatewayConstants.AUTHORIZATION_PREFIX + token;
    }

    private URI resolveUri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(config.getBaseUri() + normalizedPath);
    }

    private void validatePaymentCreateRequest(PaymentCreateRequest request) {
        requireObject(request, "payment request");
        requireText(request.getOrderNo(), "orderNo");
        requireText(request.getCurrency(), "currency");
        requireObject(request.getAmount(), "amount");
    }

    private void validatePayoutCreateRequest(PayoutCreateRequest request) {
        requireObject(request, "payout request");
        requireText(request.getOrderNo(), "orderNo");
        requireText(request.getCurrency(), "currency");
        requireObject(request.getAmount(), "amount");
        requireText(request.getPaymentMethod(), "paymentMethod");
        requireObject(request.getPaymentMethodData(), "paymentMethodData");
    }

    private void validateRefundCreateRequest(RefundCreateRequest request) {
        requireObject(request, "refund request");
        requireText(request.getTradeNo(), "tradeNo");
        requireText(request.getCurrency(), "currency");
        requireObject(request.getAmount(), "amount");
        requireObject(request.getRefundAmount(), "refundAmount");
        requireText(request.getRefundReason(), "refundReason");
    }

    private void validateCustomerCreateRequest(CustomerCreateRequest request) {
        requireObject(request, "customer request");
        requireText(request.getFirstname(), "firstname");
        requireText(request.getLastname(), "lastname");
        requireText(request.getEmail(), "email");
        requireText(request.getCountry(), "country");
    }

    private static <T> T requireObject(T value, String fieldName) {
        if (value == null) {
            throw new PaymentGatewayValidationException(fieldName + " can not be null");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new PaymentGatewayValidationException(fieldName + " can not be blank");
        }
        return value.trim();
    }

    private static String encodePath(String value) {
        return encodeQuery(value).replace("+", "%20");
    }

    private static String encodeQuery(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception exception) {
            throw new PaymentGatewayValidationException("value can not be encoded", exception);
        }
    }
}
