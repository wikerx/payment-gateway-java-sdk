package com.scott.payment.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.scott.payment.sdk.api.OpenApiEndpoint;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.crypto.OpenApiPayloadCrypto;
import com.scott.payment.sdk.crypto.RsaKeyUtils;
import com.scott.payment.sdk.exception.OpenApiHttpException;
import com.scott.payment.sdk.exception.OpenApiResponseException;
import com.scott.payment.sdk.exception.OpenApiValidationException;
import com.scott.payment.sdk.http.HttpTransport;
import com.scott.payment.sdk.http.Jdk8HttpTransport;
import com.scott.payment.sdk.http.SdkHttpRequest;
import com.scott.payment.sdk.http.SdkHttpResponse;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.jwt.MerchantJwtSigner;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.balance.BalanceResponse;
import com.scott.payment.sdk.model.common.OpenApiEncryptedRequest;
import com.scott.payment.sdk.model.common.OpenApiEncryptedResponse;
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
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
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
 * @classname : OpenApiClient
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 商户 OpenAPI Java SDK 客户端，负责请求签名、请求加密、响应解密、HTTP 调用和基础参数校验。
 *                本类不负责商户业务幂等落库、资金状态流转或渠道回调处理；支付、退款、代付、余额和客户等商户 OpenAPI 请求会按服务端最新协议使用 Bearer JWT 与 JWE data。
 *                配置中包含 API 私钥、平台请求公钥和商户响应私钥；沙盒联调开启原始日志后会输出完整 Header、明文请求、密文请求、密文响应和解密响应。
 * @status : create
 */
@Slf4j
public class OpenApiClient {

    /**
     * HTTP 调用后的密文响应和链路 requestId。
     *
     * 该对象只在 SDK 内部传递，不暴露给商户；不涉及资金状态修改或外部渠道调用。
     */
    private static final class EncryptedCallResult {

        /**
         * 网关密文响应外壳。
         */
        private final OpenApiEncryptedResponse response;

        /**
         * 本次 SDK 调用链路请求 ID。
         */
        private final String requestId;

        private EncryptedCallResult(OpenApiEncryptedResponse response, String requestId) {
            this.response = response;
            this.requestId = requestId;
        }

        private OpenApiEncryptedResponse getResponse() {
            return response;
        }

        private String getRequestId() {
            return requestId;
        }
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
    private final OpenApiClientConfig config;
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
    public OpenApiClient(OpenApiClientConfig config) {
        this(config, new Jdk8HttpTransport(), new OpenApiPayloadCrypto(), new MerchantJwtSigner());
    }

    /**
     * 从默认 classpath 配置文件创建客户端。
     *
     * @return SDK 客户端
     */
    public static OpenApiClient create() {
        return new OpenApiClient(MerchantConfigLoader.load());
    }

    /**
     * 使用自定义 HTTP 传输层创建客户端。
     *
     * @param config SDK 客户端配置
     * @param httpTransport HTTP 传输实现
     */
    public OpenApiClient(OpenApiClientConfig config, HttpTransport httpTransport) {
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
    public OpenApiClient(OpenApiClientConfig config,
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
    public OpenApiResult<PaymentResponse> createCheckoutPayment(CheckoutPaymentRequest request) {
        return createPayment(request);
    }

    /**
     * 创建本地支付直连交易。
     *
     * @param request 本地支付请求
     * @return 代收交易响应
     */
    public OpenApiResult<PaymentResponse> createLocalPayment(LocalPaymentRequest request) {
        return createPayment(request);
    }

    /**
     * 创建信用卡直连交易。
     *
     * @param request 信用卡支付请求
     * @return 代收交易响应
     */
    public OpenApiResult<PaymentResponse> createCardPayment(CardPaymentRequest request) {
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
    public OpenApiResult<PaymentResponse> retrievePayment(String tradeNo) {
        return getSecured(OpenApiEndpoint.PAYMENT_RETRIEVE,
                PaymentResponse.class,
                "query-" + UUID.randomUUID(),
                encodePath(requireText(tradeNo, "tradeNo")));
    }

    /**
     * 创建代付交易。
     *
     * @param request 代付创建请求
     * @return 代付交易响应
     */
    public OpenApiResult<PayoutResponse> createPayout(PayoutCreateRequest request) {
        validatePayoutCreateRequest(request);
        return postEncrypted(OpenApiEndpoint.PAYOUT_TRANSFER_CREATE,
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
    public OpenApiResult<PayoutResponse> retrievePayout(String tradeNo) {
        return getSecured(OpenApiEndpoint.PAYOUT_TRANSFER_RETRIEVE,
                PayoutResponse.class,
                "query-" + UUID.randomUUID(),
                encodePath(requireText(tradeNo, "tradeNo")));
    }

    /**
     * 取消代付交易。
     *
     * @param request 代付取消请求
     * @return 代付取消响应
     */
    public OpenApiResult<PayoutCancelResponse> cancelPayout(PayoutCancelRequest request) {
        requireObject(request, "request");
        String jti = StringUtils.defaultIfBlank(request.getTradeNo(), request.getOrderNo());
        return postEncrypted(OpenApiEndpoint.PAYOUT_TRANSFER_CANCEL,
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
    public OpenApiResult<RefundResponse> createRefund(RefundCreateRequest request) {
        validateRefundCreateRequest(request);
        String jti = StringUtils.defaultIfBlank(request.getCharge(),
                request.getTradeNo() + "-" + request.getRefundAmount());
        return postEncrypted(OpenApiEndpoint.REFUND_CREATE, request, RefundResponse.class, jti);
    }

    /**
     * 查询退款。
     *
     * @param refundNo 退款标识符
     * @return 退款响应
     */
    public OpenApiResult<RefundResponse> retrieveRefund(String refundNo) {
        return getSecured(OpenApiEndpoint.REFUND_RETRIEVE,
                RefundResponse.class,
                "query-" + UUID.randomUUID(),
                encodePath(requireText(refundNo, "refundNo")));
    }

    /**
     * 查询商户全部余额。
     *
     * @return 余额响应列表
     */
    public OpenApiResult<List<BalanceResponse>> retrieveBalances() {
        return getListSecured(OpenApiEndpoint.FUND_ACCOUNTS_BALANCE_INQUIRY,
                OpenApiEndpoint.FUND_ACCOUNTS_BALANCE_INQUIRY.getPath(),
                BalanceResponse.class,
                "balance-" + UUID.randomUUID());
    }

    /**
     * 按币种查询商户余额。
     *
     * @param currency 币种
     * @return 余额响应列表
     */
    public OpenApiResult<List<BalanceResponse>> retrieveBalances(String currency) {
        String path = OpenApiEndpoint.FUND_ACCOUNTS_BALANCE_INQUIRY.getPath()
                + "?currency=" + encodeQuery(requireText(currency, "currency"));
        return getListSecured(OpenApiEndpoint.FUND_ACCOUNTS_BALANCE_INQUIRY,
                path,
                BalanceResponse.class,
                "balance-" + UUID.randomUUID());
    }

    /**
     * 创建客户。
     *
     * @param request 客户创建请求
     * @return 客户响应
     */
    public OpenApiResult<CustomerResponse> createCustomer(CustomerCreateRequest request) {
        validateCustomerCreateRequest(request);
        String jti = "customer-" + StringUtils.defaultIfBlank(request.getEmail(), UUID.randomUUID().toString());
        return postEncrypted(OpenApiEndpoint.CUSTOMER_CREATE, request, CustomerResponse.class, jti);
    }

    /**
     * 查询客户。
     *
     * @param customerId 客户 ID
     * @return 客户响应
     */
    public OpenApiResult<CustomerResponse> retrieveCustomer(String customerId) {
        return getSecured(OpenApiEndpoint.CUSTOMER_RETRIEVE,
                CustomerResponse.class,
                "query-" + UUID.randomUUID(),
                encodePath(requireText(customerId, "customerId")));
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
    public <T> OpenApiResult<T> postEncrypted(OpenApiEndpoint api,
                                                      Object request,
                                                      Class<T> responseType,
                                                      String jwtId) {
        requireObject(request, "request");
        OpenApiEncryptedRequest encryptedRequest = encryptRequest(request);
        return execute(api,
                api.getPath(),
                request,
                encryptedRequest,
                responseType,
                jwtId);
    }

    /**
     * 发送新协议 GET 请求，使用 Bearer JWT 并自动解密响应 data。
     *
     * @param api API 元数据
     * @param responseType 响应 data 类型
     * @param jwtId JWT jti
     * @param pathArgs 路径模板参数
     * @param <T> 响应 data 类型
     * @return SDK 响应
     */
    public <T> OpenApiResult<T> getSecured(OpenApiEndpoint api,
                                                  Class<T> responseType,
                                                  String jwtId,
                                                  Object... pathArgs) {
        return execute(api,
                api.formatPath(pathArgs),
                null,
                null,
                responseType,
                jwtId);
    }

    /**
     * 发送新协议 GET 请求并解析列表 data。
     *
     * @param api API 元数据
     * @param path 接口路径，允许带 query string
     * @param elementType 列表元素类型
     * @param jwtId JWT jti
     * @param <T> 列表元素类型
     * @return SDK 列表响应
     */
    public <T> OpenApiResult<List<T>> getListSecured(OpenApiEndpoint api,
                                                            String path,
                                                            Class<T> elementType,
                                                            String jwtId) {
        EncryptedCallResult encryptedCallResult = executeRaw(api, path, null, null, jwtId);
        return convertListResult(api,
                encryptedCallResult.getResponse(),
                elementType,
                encryptedCallResult.getRequestId());
    }

    private OpenApiResult<PaymentResponse> createPayment(PaymentCreateRequest request) {
        validatePaymentCreateRequest(request);
        return postEncrypted(OpenApiEndpoint.PAYMENT_CREATE,
                request,
                PaymentResponse.class,
                requireText(request.getOrderNo(), "orderNo"));
    }

    /**
     * 加密商户业务请求对象。
     *
     * 该方法会把业务 DTO 序列化为 JSON，再使用平台请求公钥生成 compact payload，并封装为 `livemode + data`。
     * 方法不发起 HTTP 请求、不修改资金或业务状态；明文 JSON 只在本地内存中参与加密，不应进入普通日志。
     *
     * @param plainRequest 商户业务请求 DTO
     * @return OpenAPI 密文请求外壳
     */
    private OpenApiEncryptedRequest encryptRequest(Object plainRequest) {
        String requestJson = JsonSupport.toJson(plainRequest);
        String encryptedData = payloadCrypto.encrypt(requestJson, platformPublicKey);
        return OpenApiEncryptedRequest.builder()
                .livemode(config.getLivemode())
                .data(encryptedData)
                .build();
    }

    /**
     * 执行 OpenAPI 调用并转换为商户可读响应对象。
     *
     * 该方法编排 HTTP 调用、密文响应解析、响应 data 解密和业务 DTO 反序列化；不负责商户订单幂等落库或资金状态确认。
     *
     * @param api API 元数据
     * @param path 请求路径，可包含 query string
     * @param plainRequest 原始业务请求对象，GET 请求为空
     * @param encryptedRequest 加密请求外壳，GET 请求为空
     * @param responseType 响应 data 目标类型
     * @param jwtId JWT jti，参与防重放
     * @param <T> 响应 data 类型
     * @return SDK 通用响应对象
     */
    private <T> OpenApiResult<T> execute(OpenApiEndpoint api,
                                                String path,
                                                Object plainRequest,
                                                OpenApiEncryptedRequest encryptedRequest,
                                                Class<T> responseType,
                                                String jwtId) {
        EncryptedCallResult encryptedCallResult = executeRaw(api, path, plainRequest, encryptedRequest, jwtId);
        return convertResult(api,
                encryptedCallResult.getResponse(),
                responseType,
                encryptedCallResult.getRequestId());
    }

    /**
     * 执行底层 HTTP 请求并返回网关密文响应外壳。
     *
     * 该方法负责生成 requestId、Bearer JWT、请求 Header 和 HTTP body，并记录标准日志；不会解密业务 data。
     * 支付、退款、代付请求可能已经到达网关，遇到网络异常时调用方应使用查询接口确认最终状态。
     *
     * @param api API 元数据
     * @param path 请求路径，可包含 query string
     * @param plainRequest 原始业务请求对象，仅用于调试日志
     * @param encryptedRequest 加密请求外壳，GET 请求为空
     * @param jwtId JWT jti，参与防重放
     * @return 密文响应和本次 requestId
     */
    private EncryptedCallResult executeRaw(OpenApiEndpoint api,
                                           String path,
                                           Object plainRequest,
                                           OpenApiEncryptedRequest encryptedRequest,
                                           String jwtId) {
        String requestId = UUID.randomUUID().toString();
        long startMillis = System.currentTimeMillis();
        URI requestUri = resolveUri(path);
        String body = encryptedRequest == null ? null : JsonSupport.toJson(encryptedRequest);
        Map<String, String> requestHeaders = headers(jwtId, requestId, body != null);
        logApiStart(api, path, requestId);
        logRequestAddress(requestUri);
        logPlainRequest(plainRequest);
        logRequestPayloadComponents(encryptedRequest);
        logEncryptedRequest(encryptedRequest);
        try {
            SdkHttpResponse response = httpTransport.execute(SdkHttpRequest.builder()
                    .method(api.getMethod())
                    .uri(requestUri)
                    .headers(requestHeaders)
                    .body(body)
                    .connectTimeoutMs(config.getConnectTimeoutMs())
                    .readTimeoutMs(config.getReadTimeoutMs())
                    .build());
            OpenApiEncryptedResponse encryptedResponse = parseEncryptedResponse(response);
            if (response.getStatusCode() < OpenApiConstants.HTTP_STATUS_SUCCESS_MIN
                    || response.getStatusCode() >= OpenApiConstants.HTTP_STATUS_SUCCESS_MAX_EXCLUSIVE) {
                logApiEnd(api, path, requestId, response.getStatusCode(), startMillis, false);
                throw new OpenApiHttpException("OpenAPI HTTP status is not successful: " + response.getStatusCode());
            }
            logApiEnd(api, path, requestId, response.getStatusCode(), startMillis, true);
            logEncryptedResponse(response, encryptedResponse);
            logResponsePayloadComponents(encryptedResponse);
            return new EncryptedCallResult(encryptedResponse, requestId);
        } catch (RuntimeException exception) {
            log.warn("请求异常: {}", JsonSupport.toJson(logFields(
                    "apiName", api.getApiName(),
                    "method", api.getMethod(),
                "path", path,
                    "merchantId", config.getMerchantId(),
                    "requestId", requestId,
                    "elapsedMillis", System.currentTimeMillis() - startMillis,
                    "errorType", exception.getClass().getSimpleName(),
                    "message", "OpenAPI request failed")));
            throw exception;
        }
    }

    private void logApiStart(OpenApiEndpoint api, String path, String requestId) {
        logJson("API调用开始", logFields(
                "apiName", api.getApiName(),
                "method", api.getMethod(),
                "path", path,
                "merchantId", config.getMerchantId(),
                "requestId", requestId));
    }

    private void logApiEnd(OpenApiEndpoint api,
                           String path,
                           String requestId,
                           int statusCode,
                           long startMillis,
                           boolean success) {
        logJson("API调用结束", logFields(
                "apiName", api.getApiName(),
                "method", api.getMethod(),
                "path", path,
                "merchantId", config.getMerchantId(),
                "requestId", requestId,
                "statusCode", statusCode,
                "elapsedMillis", System.currentTimeMillis() - startMillis,
                "success", success));
    }

    private void logPlainRequest(Object plainRequest) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || plainRequest == null) {
            return;
        }
        logJson("请求原始明文报文", OpenApiLogSanitizer.sanitizeObject(plainRequest));
    }

    private void logEncryptedRequest(OpenApiEncryptedRequest encryptedRequest) {
        if (encryptedRequest == null) {
            return;
        }
        Object requestForLog = encryptedRequest;
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            requestForLog = logFields(
                    "livemode", encryptedRequest.getLivemode(),
                    "data", OpenApiLogSanitizer.encryptedDataSummary(encryptedRequest.getData()));
        }
        logJson("请求密文参数", requestForLog);
    }

    private void logRequestAddress(URI requestUri) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            return;
        }
        log.info("请求地址: {}", requestUri.toString());
        log.debug("请求地址: {}", requestUri.toString());
    }

    private void logEncryptedResponse(SdkHttpResponse response, OpenApiEncryptedResponse encryptedResponse) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            return;
        }
        logJson("响应原始密文参数", logFields(
                "statusCode", response.getStatusCode(),
                "headers", OpenApiLogSanitizer.sanitizeHeaders(response.getHeaders()),
                "body", encryptedResponse));
    }

    private void logPlainResponse(Object responseData) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || responseData == null) {
            return;
        }
        logJson("响应原始明文参数", OpenApiLogSanitizer.sanitizeObject(responseData));
    }

    private void logRequestPayloadComponents(OpenApiEncryptedRequest encryptedRequest) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || encryptedRequest == null) {
            return;
        }
        logEncryptedPayloadComponents("请求参数拆分", encryptedRequest.getData());
    }

    private void logResponsePayloadComponents(OpenApiEncryptedResponse encryptedResponse) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || encryptedResponse == null) {
            return;
        }
        logEncryptedPayloadComponents("响应参数拆分", encryptedResponse.getData());
    }

    private void logEncryptedPayloadComponents(String logName, String compactPayload) {
        Map<String, String> components = compactPayloadComponentsForLog(compactPayload);
        if (components.isEmpty()) {
            logJson(logName + "跳过", logFields("reason", "invalid_compact_payload"));
            return;
        }
        logJson(logName, logFields(
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

    private <T> OpenApiResult<T> convertResult(OpenApiEndpoint api,
                                                       OpenApiEncryptedResponse encryptedResponse,
                                                       Class<T> responseType,
                                                       String requestId) {
        validateResponseLivemode(encryptedResponse.getLivemode());
        OpenApiResult<T> result = new OpenApiResult<T>();
        result.setCode(encryptedResponse.getCode());
        result.setMsg(encryptedResponse.getMsg());
        result.setLivemode(encryptedResponse.getLivemode());
        if (StringUtils.isNotBlank(encryptedResponse.getData())) {
            String plainJson = decryptResponseData(encryptedResponse.getData());
            T data = JsonSupport.fromJson(plainJson, responseType);
            result.setData(data);
            logPlainResponse(data);
        }
        return result;
    }

    private <T> OpenApiResult<List<T>> convertListResult(OpenApiEndpoint api,
                                                                OpenApiEncryptedResponse encryptedResponse,
                                                                Class<T> elementType,
                                                                String requestId) {
        validateResponseLivemode(encryptedResponse.getLivemode());
        OpenApiResult<List<T>> result = new OpenApiResult<List<T>>();
        result.setCode(encryptedResponse.getCode());
        result.setMsg(encryptedResponse.getMsg());
        result.setLivemode(encryptedResponse.getLivemode());
        if (StringUtils.isNotBlank(encryptedResponse.getData())) {
            String plainJson = decryptResponseData(encryptedResponse.getData());
            JsonNode plainNode = JsonSupport.fromJson(plainJson, JsonNode.class);
            if (plainNode.isArray()) {
                result.setData(JsonSupport.fromJsonList(plainJson, elementType));
            } else {
                result.setData(java.util.Collections.singletonList(JsonSupport.fromJson(plainJson, elementType)));
            }
            logPlainResponse(result.getData());
        }
        return result;
    }

    /**
     * 解析网关密文响应外壳。
     *
     * 该方法只解析 code、msg、livemode 和密文 data，不执行解密，也不修改业务状态。
     *
     * @param response HTTP 响应
     * @return OpenAPI 密文响应外壳
     */
    private OpenApiEncryptedResponse parseEncryptedResponse(SdkHttpResponse response) {
        return JsonSupport.fromJson(response.getBody(), OpenApiEncryptedResponse.class);
    }

    /**
     * 解密网关响应 data。
     *
     * 该方法使用商户响应私钥解密 compact payload，返回业务响应 JSON 明文；不做业务 DTO 映射，也不修改资金状态。
     *
     * @param encryptedData 响应 compact 密文 data
     * @return 解密后的业务响应 JSON
     */
    private String decryptResponseData(String encryptedData) {
        return payloadCrypto.decrypt(encryptedData, merchantResponsePrivateKey);
    }

    private void validateResponseLivemode(Boolean responseLivemode) {
        if (responseLivemode != null && !responseLivemode.equals(config.getLivemode())) {
            throw new OpenApiResponseException("OpenAPI response livemode is inconsistent");
        }
    }

    /**
     * 构造 OpenAPI HTTP Header。
     *
     * 该方法会生成 Authorization Bearer JWT，并写入 Accept、User-Agent、X-Request-Id 和必要的 Content-Type。
     * Header 生成不修改资金或业务状态；日志输出前必须脱敏 Authorization。
     *
     * @param jwtId JWT jti，参与防重放
     * @param requestId SDK 本地链路请求 ID
     * @param withBody 是否存在请求体
     * @return HTTP Header Map
     */
    private Map<String, String> headers(String jwtId, String requestId, boolean withBody) {
        Map<String, String> headers = new HashMap<String, String>(OpenApiConstants.HTTP_HEADER_MAP_SIZE);
        headers.put(OpenApiConstants.HEADER_AUTHORIZATION, authorizationValue(jwtId));
        headers.put(OpenApiConstants.HEADER_ACCEPT, OpenApiConstants.ACCEPT);
        headers.put(OpenApiConstants.HEADER_USER_AGENT, OpenApiConstants.USER_AGENT);
        headers.put(OpenApiConstants.HEADER_REQUEST_ID, requestId);
        if (withBody) {
            headers.put(OpenApiConstants.HEADER_CONTENT_TYPE, OpenApiConstants.CONTENT_TYPE);
        }
        String headersJson = JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeHeaders(headers));
        log.info("请求头: {}", headersJson);
        log.debug("请求头: {}", headersJson);

        return headers;
    }

    /**
     * 生成 Authorization Header 值。
     *
     * 该方法使用商户 API 私钥签发 HS256 JWT，JWT 包含 merchantId、livemode、jti、iat 和 exp。
     * 该值属于敏感鉴权材料，普通日志只能输出脱敏结果。
     *
     * @param jwtId JWT jti，参与防重放
     * @return `Bearer <jwt>` 格式的 Header 值
     */
    private String authorizationValue(String jwtId) {
        Date now = new Date(config.getClock().millis());
        String token = jwtSigner.sign(
                config.getMerchantId(),
                config.getMerchantJwtSecret(),
                config.getLivemode(),
                requireText(jwtId, "jwtId"),
                now,
                config.getJwtTtlSeconds());
        return OpenApiConstants.AUTHORIZATION_PREFIX + token;
    }

    private void logJson(String name, Object value) {
        String json = JsonSupport.toLogJson(value);
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
            throw new OpenApiValidationException(fieldName + " can not be null");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new OpenApiValidationException(fieldName + " can not be blank");
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
            throw new OpenApiValidationException("value can not be encoded", exception);
        }
    }
}
