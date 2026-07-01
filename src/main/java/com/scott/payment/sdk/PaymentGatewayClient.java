package com.scott.payment.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.config.PaymentGatewayConstants;
import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.crypto.RsaKeyUtils;
import com.scott.payment.sdk.exception.PaymentGatewayHttpException;
import com.scott.payment.sdk.exception.PaymentGatewayResponseException;
import com.scott.payment.sdk.exception.PaymentGatewayValidationException;
import com.scott.payment.sdk.http.HttpTransport;
import com.scott.payment.sdk.http.Jdk8HttpTransport;
import com.scott.payment.sdk.http.SdkHttpRequest;
import com.scott.payment.sdk.http.SdkHttpResponse;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.jwt.MerchantJwtSigner;
import com.scott.payment.sdk.logging.PaymentGatewayLogSanitizer;
import com.scott.payment.sdk.model.balance.BalanceResponse;
import com.scott.payment.sdk.model.common.EncryptedRequest;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import com.scott.payment.sdk.model.payment.CardPaymentRequest;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.LocalPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentCreateRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.model.payout.PayoutCancelRequest;
import com.scott.payment.sdk.model.payout.PayoutCancelResponse;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import com.scott.payment.sdk.model.payout.PayoutResponse;
import com.scott.payment.sdk.model.refund.RefundCreateRequest;
import com.scott.payment.sdk.model.refund.RefundResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentGatewayClient
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 商户支付网关 Java SDK 客户端，负责请求签名、请求加密、响应解密、HTTP 调用和基础参数校验。
 *                本类不负责商户业务幂等落库、资金状态流转或渠道回调处理；支付、退款、代付、余额和客户等商户 OpenAPI 请求会按服务端最新协议使用 Bearer JWT 与 JWE data。
 *                配置中包含 API 私钥、平台请求公钥和商户响应私钥；沙盒联调开启原始日志后会输出完整 Header、明文请求、密文请求、密文响应和解密响应。
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
     * compact 加密 payload 固定分段数：protectedHeader.encryptedAesKey.iv.cipherText.tag。
     */
    private static final int COMPACT_PAYLOAD_PARTS = 5;

    /**
     * compact protected header 字段名。
     */
    private static final String COMPACT_PROTECTED_HEADER = "protectedHeader";

    /**
     * compact protected header 解码后的 JSON 字段名。
     */
    private static final String COMPACT_HEADER = "header";

    /**
     * compact RSA-OAEP 加密后的 AES 会话密钥字段名。
     */
    private static final String COMPACT_ENCRYPTED_AES_KEY = "encryptedAesKey";

    /**
     * compact AES-GCM IV 字段名。
     */
    private static final String COMPACT_IV = "iv";

    /**
     * compact AES-GCM 密文字段名。
     */
    private static final String COMPACT_CIPHER_TEXT = "cipherText";

    /**
     * compact AES-GCM 认证标签字段名。
     */
    private static final String COMPACT_TAG = "tag";

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
        return getSecured(String.format(PaymentGatewayConstants.PAYMENT_RETRIEVE_PATH, encodePath(requireText(tradeNo, "tradeNo"))),
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
        return postEncrypted(PaymentGatewayConstants.PAYOUT_CREATE_PATH,
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
        return getSecured(String.format(PaymentGatewayConstants.PAYOUT_RETRIEVE_PATH, encodePath(requireText(tradeNo, "tradeNo"))),
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
        return postEncrypted(PaymentGatewayConstants.PAYOUT_CANCEL_PATH,
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
        return postEncrypted(PaymentGatewayConstants.REFUND_CREATE_PATH, request, RefundResponse.class, jti);
    }

    /**
     * 查询退款。
     *
     * @param refundNo 退款标识符
     * @return 退款响应
     */
    public PaymentGatewayResult<RefundResponse> retrieveRefund(String refundNo) {
        return getSecured(String.format(PaymentGatewayConstants.REFUND_RETRIEVE_PATH, encodePath(requireText(refundNo, "refundNo"))),
                RefundResponse.class,
                "query-" + UUID.randomUUID());
    }

    /**
     * 查询商户全部余额。
     *
     * @return 余额响应列表
     */
    public PaymentGatewayResult<List<BalanceResponse>> retrieveBalances() {
        return getListSecured(PaymentGatewayConstants.BALANCE_RETRIEVE_PATH,
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
        logValue("检索余额请求地址", path);
        return getListSecured(path, BalanceResponse.class, "balance-" + UUID.randomUUID());
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
        return postEncrypted(PaymentGatewayConstants.CUSTOMER_CREATE_PATH, request, CustomerResponse.class, jti);
    }

    /**
     * 查询客户。
     *
     * @param customerId 客户 ID
     * @return 客户响应
     */
    public PaymentGatewayResult<CustomerResponse> retrieveCustomer(String customerId) {
        return getSecured(String.format(PaymentGatewayConstants.CUSTOMER_RETRIEVE_PATH, encodePath(requireText(customerId, "customerId"))),
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
        return execute("POST",
                path,
                JsonSupport.toJson(new EncryptedRequest(config.getLivemode(), encryptedData)),
                requestJson,
                responseType,
                jwtId,
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
     * 发送新协议 GET 请求，使用 Bearer JWT 并自动解密响应 data。
     *
     * @param path 接口路径
     * @param responseType 响应 data 类型
     * @param jwtId JWT jti
     * @param <T> 响应 data 类型
     * @return SDK 响应
     */
    public <T> PaymentGatewayResult<T> getSecured(String path, Class<T> responseType, String jwtId) {
        return execute("GET", path, null, responseType, jwtId, AuthorizationMode.BEARER_JWT);
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

    /**
     * 发送新协议 GET 请求并解析列表 data。
     *
     * @param path 接口路径
     * @param elementType 列表元素类型
     * @param jwtId JWT jti
     * @param <T> 列表元素类型
     * @return SDK 列表响应
     */
    public <T> PaymentGatewayResult<List<T>> getListSecured(String path, Class<T> elementType, String jwtId) {
        PaymentGatewayResult<JsonNode> rawResult = executeRaw("GET", path, null, jwtId, AuthorizationMode.BEARER_JWT);
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
        return execute(method, path, body, null, responseType, jwtId, authorizationMode);
    }

    private <T> PaymentGatewayResult<T> execute(String method,
                                                String path,
                                                String body,
                                                String plainBody,
                                                Class<T> responseType,
                                                String jwtId,
                                                AuthorizationMode authorizationMode) {
        PaymentGatewayResult<JsonNode> rawResult = executeRaw(method, path, body, plainBody, jwtId, authorizationMode);
        return convertResult(rawResult, responseType);
    }

    private PaymentGatewayResult<JsonNode> executeRaw(String method,
                                                      String path,
                                                      String body,
                                                      String jwtId,
                                                      AuthorizationMode authorizationMode) {
        return executeRaw(method, path, body, null, jwtId, authorizationMode);
    }

    private PaymentGatewayResult<JsonNode> executeRaw(String method,
                                                      String path,
                                                      String body,
                                                      String plainBody,
                                                      String jwtId,
                                                      AuthorizationMode authorizationMode) {
        String requestId = UUID.randomUUID().toString();
        long startMillis = System.currentTimeMillis();
        URI requestUri = resolveUri(path);
        Map<String, String> requestHeaders = headers(jwtId, requestId, body != null, authorizationMode);
        logJson("请求开始", logFields(
                "method", method,
                "path", path,
                "merchantId", config.getMerchantId(),
                "requestId", requestId));
        logJson("请求参数摘要", logFields(
                "requestId", requestId,
                "summary", PaymentGatewayLogSanitizer.bodySummary(body)));
        logRawPlainRequest(method, requestUri, plainBody, requestId);
        logRawRequest(method, requestUri, requestHeaders, body, requestId);
        try {
            SdkHttpResponse response = httpTransport.execute(SdkHttpRequest.builder()
                    .method(method)
                    .uri(requestUri)
                    .headers(requestHeaders)
                    .body(body)
                    .connectTimeoutMs(config.getConnectTimeoutMs())
                    .readTimeoutMs(config.getReadTimeoutMs())
                    .build());
            if (response.getStatusCode() < PaymentGatewayConstants.HTTP_STATUS_SUCCESS_MIN
                    || response.getStatusCode() >= PaymentGatewayConstants.HTTP_STATUS_SUCCESS_MAX_EXCLUSIVE) {
                log.warn("请求结束: {}", JsonSupport.toJson(logFields(
                        "method", method,
                        "path", path,
                        "merchantId", config.getMerchantId(),
                        "requestId", requestId,
                        "statusCode", response.getStatusCode(),
                        "elapsedMillis", System.currentTimeMillis() - startMillis,
                        "success", false)));
                throw new PaymentGatewayHttpException("Payment Gateway HTTP status is not successful: " + response.getStatusCode());
            }
            logJson("请求结束", logFields(
                    "method", method,
                    "path", path,
                    "merchantId", config.getMerchantId(),
                    "requestId", requestId,
                    "statusCode", response.getStatusCode(),
                    "elapsedMillis", System.currentTimeMillis() - startMillis,
                    "success", true));
            logJson("响应参数摘要", logFields(
                    "requestId", requestId,
                    "summary", PaymentGatewayLogSanitizer.bodySummary(response.getBody())));
            logRawResponse(response, requestId);
            return JsonSupport.fromJson(response.getBody(), new TypeReference<PaymentGatewayResult<JsonNode>>() {
            });
        } catch (RuntimeException exception) {
            log.warn("请求异常: {}", JsonSupport.toJson(logFields(
                    "method", method,
                    "path", path,
                    "merchantId", config.getMerchantId(),
                    "requestId", requestId,
                    "elapsedMillis", System.currentTimeMillis() - startMillis,
                    "errorType", exception.getClass().getSimpleName(),
                    "message", "Payment Gateway request failed")));
            throw exception;
        }
    }

    private void logRawRequest(String method, URI requestUri, Map<String, String> requestHeaders, String body, String requestId) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            return;
        }
        logJson("请求地址", logFields(
                "requestId", requestId,
                "method", method,
                "url", requestUri.toString()));
        logJson("原始请求头", logFields(
                "requestId", requestId,
                "headers", requestHeaders));
        logJson("请求参数加密", logFields(
                "requestId", requestId,
                "body", body));
        logEncryptedPayloadComponentsFromEnvelope("request", requestId, body);
    }

    private void logRawPlainRequest(String method, URI requestUri, String plainBody, String requestId) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            return;
        }
        logJson("原始请求参数", logFields(
                "requestId", requestId,
                "method", method,
                "url", requestUri.toString(),
                "body", plainBody));
    }

    private void logRawResponse(SdkHttpResponse response, String requestId) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            return;
        }
        logJson("原始响应参数", logFields(
                "requestId", requestId,
                "statusCode", response.getStatusCode(),
                "headers", response.getHeaders(),
                "body", response.getBody()));
        logEncryptedPayloadComponentsFromEnvelope("response", requestId, response.getBody());
    }

    private void logRawPlainResponseData(String dataJson) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            return;
        }
        logJson("响应参数解密", logFields("data", dataJson));
    }

    private void logEncryptedPayloadComponentsFromEnvelope(String direction, String requestId, String body) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || StringUtils.isBlank(body)) {
            return;
        }
        try {
            JsonNode dataNode = JsonSupport.fromJson(body, JsonNode.class).get("data");
            if (dataNode != null && dataNode.isTextual()) {
                logEncryptedPayloadComponents(direction, requestId, dataNode.asText());
            }
        } catch (RuntimeException exception) {
            logJson("加密参数拆分跳过", logFields(
                    "direction", direction,
                    "requestId", requestId,
                    "reason", "body_parse_failed"));
        }
    }

    private void logEncryptedPayloadComponents(String direction, String requestId, String compactPayload) {
        Map<String, String> components = compactPayloadComponentsForLog(compactPayload);
        if (components.isEmpty()) {
            logJson("加密参数拆分跳过", logFields(
                    "direction", direction,
                    "requestId", requestId,
                    "reason", "invalid_compact_payload"));
            return;
        }
        logJson("加密参数拆分", logFields(
                "direction", direction,
                "requestId", requestId,
                COMPACT_PROTECTED_HEADER, components.get(COMPACT_PROTECTED_HEADER),
                COMPACT_HEADER, components.get(COMPACT_HEADER),
                COMPACT_ENCRYPTED_AES_KEY, components.get(COMPACT_ENCRYPTED_AES_KEY),
                COMPACT_IV, components.get(COMPACT_IV),
                COMPACT_CIPHER_TEXT, components.get(COMPACT_CIPHER_TEXT),
                COMPACT_TAG, components.get(COMPACT_TAG)));
    }

    static Map<String, String> compactPayloadComponentsForLog(String compactPayload) {
        if (StringUtils.isBlank(compactPayload)) {
            return java.util.Collections.emptyMap();
        }
        String[] parts = compactPayload.split("\\.", -1);
        if (parts.length != COMPACT_PAYLOAD_PARTS) {
            return java.util.Collections.emptyMap();
        }
        Map<String, String> components = new LinkedHashMap<String, String>();
        components.put(COMPACT_PROTECTED_HEADER, parts[0]);
        components.put(COMPACT_HEADER, decodeProtectedHeaderForLog(parts[0]));
        components.put(COMPACT_ENCRYPTED_AES_KEY, parts[1]);
        components.put(COMPACT_IV, parts[2]);
        components.put(COMPACT_CIPHER_TEXT, parts[3]);
        components.put(COMPACT_TAG, parts[4]);
        return components;
    }

    private static String decodeProtectedHeaderForLog(String protectedHeader) {
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(protectedHeader);
            return new String(headerBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return protectedHeader;
        }
    }

    private <T> PaymentGatewayResult<T> convertResult(PaymentGatewayResult<JsonNode> rawResult, Class<T> responseType) {
        validateResponseLivemode(rawResult.getLivemode());
        PaymentGatewayResult<T> result = new PaymentGatewayResult<T>();
        result.setCode(rawResult.getCode());
        result.setMsg(rawResult.getMsg());
        result.setLivemode(rawResult.getLivemode());
        if (rawResult.getData() != null && !rawResult.getData().isNull()) {
            String plainJson = resolveDataJson(rawResult.getData());
            logRawPlainResponseData(plainJson);
            result.setData(JsonSupport.fromJson(plainJson, responseType));
        }
        return result;
    }

    private <T> PaymentGatewayResult<List<T>> convertListResult(PaymentGatewayResult<JsonNode> rawResult, Class<T> elementType) {
        validateResponseLivemode(rawResult.getLivemode());
        PaymentGatewayResult<List<T>> result = new PaymentGatewayResult<List<T>>();
        result.setCode(rawResult.getCode());
        result.setMsg(rawResult.getMsg());
        result.setLivemode(rawResult.getLivemode());
        if (rawResult.getData() != null && !rawResult.getData().isNull()) {
            String plainJson = resolveDataJson(rawResult.getData());
            logRawPlainResponseData(plainJson);
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

    private void validateResponseLivemode(Boolean responseLivemode) {
        if (responseLivemode != null && !responseLivemode.equals(config.getLivemode())) {
            throw new PaymentGatewayResponseException("Payment Gateway response livemode is inconsistent");
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
        String headersJson = JsonSupport.toJson(PaymentGatewayLogSanitizer.sanitizeHeaders(headers));
        log.info("请求头: {}", headersJson);
        log.debug("请求头: {}", headersJson);

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
                config.getLivemode(),
                requireText(jwtId, "jwtId"),
                now,
                config.getJwtTtlSeconds());
        return PaymentGatewayConstants.AUTHORIZATION_PREFIX + token;
    }

    private void logValue(String name, Object value) {
        log.info("{}: {}", name, value);
        log.debug("{}: {}", name, value);
    }

    private void logJson(String name, Object value) {
        String json = JsonSupport.toJson(value);
        log.info("{}: {}", name, json);
        log.debug("{}: {}", name, json);
    }

    private static Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
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
