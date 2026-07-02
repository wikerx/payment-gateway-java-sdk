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
import com.scott.payment.sdk.model.common.OpenApiPayloadParts;
import com.scott.payment.sdk.model.common.PaymentMethod;
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
     * 该构造器会使用 {@link Jdk8HttpTransport} 发起真实 HTTP 请求，适用于商户真实联调和生产调用。
     * 构造阶段会校验配置并解析平台请求公钥、商户响应私钥；不会主动调用网关，也不会修改资金、交易状态或密钥配置。
     *
     * @param config SDK 客户端配置，必须包含 base-url、商户号、livemode、JWT API 密钥、请求加密公钥和响应解密私钥
     */
    public OpenApiClient(OpenApiClientConfig config) {
        this(config, new Jdk8HttpTransport(), new OpenApiPayloadCrypto(), new MerchantJwtSigner());
    }

    /**
     * 从默认 classpath 配置文件创建客户端。
     *
     * 该方法读取 merchant-config.properties 并创建使用真实 JDK HTTP 传输层的客户端。
     * 方法本身不发起网关请求；后续调用支付、代付、退款等方法时才会访问配置中的 base-url。
     *
     * @return SDK 客户端
     */
    public static OpenApiClient create() {
        return new OpenApiClient(MerchantConfigLoader.load());
    }

    /**
     * 使用自定义 HTTP 传输层创建客户端。
     *
     * 该构造器主要用于单元测试、商户自定义网络栈或本地模拟网关场景。
     * 是否真实访问网关由传入的 {@link HttpTransport} 决定；传入测试传输层时只会模拟响应，不会创建真实交易。
     *
     * @param config SDK 客户端配置
     * @param httpTransport HTTP 传输实现，生产环境通常使用 Jdk8HttpTransport，测试可使用内存模拟实现
     */
    public OpenApiClient(OpenApiClientConfig config, HttpTransport httpTransport) {
        this(config, httpTransport, new OpenApiPayloadCrypto(), new MerchantJwtSigner());
    }

    /**
     * 使用自定义核心组件创建客户端，主要用于测试或商户高级扩展。
     *
     * 该构造器会完成配置校验和 RSA 密钥解析；不会修改资金、交易状态、商户配置或外部渠道状态。
     * 如果密钥格式错误或必要配置缺失，会在构造阶段抛出 SDK 配置或加解密异常，便于商户启动时发现问题。
     *
     * @param config SDK 客户端配置
     * @param httpTransport HTTP 传输实现，决定请求是真实发送还是测试模拟
     * @param payloadCrypto payload 加解密组件，负责 JWE compact payload 处理
     * @param jwtSigner JWT 签名组件，负责生成 Bearer JWT
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
     * 该方法会校验订单号、币种和金额，随后按最新 OpenAPI 协议生成 JWT、加密请求 data 并通过当前 HTTP 传输层发送。
     * 使用默认传输层时会真实创建网关支付交易；使用测试传输层时只验证 SDK 请求封装并返回模拟响应。
     * 本方法不处理商户侧幂等落库、资金入账、订单状态流转或渠道回调。
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
     * 该方法复用代收创建链路完成参数校验、JWT 签名、请求加密、HTTP 调用和响应解密。
     * 是否真实访问网关取决于客户端构造时注入的 HTTP 传输层；本方法不负责商户侧幂等、资金状态确认或渠道回调处理。
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
     * 如果请求未显式设置 paymentMethod，SDK 会默认填充 CARD，再走代收创建加密调用链路。
     * 本方法涉及卡号、CVC 等敏感数据，SDK 只负责请求加密和日志脱敏，不保存卡信息、不修改商户订单状态。
     *
     * @param request 信用卡支付请求
     * @return 代收交易响应
     */
    public OpenApiResult<PaymentResponse> createCardPayment(CardPaymentRequest request) {
        if (request != null && StringUtils.isBlank(request.getPaymentMethod())) {
            request.setPaymentMethod(PaymentMethod.CARD);
        }
        return createPayment(request);
    }

    /**
     * 查询代收交易。
     *
     * 该方法通过 GET + Bearer JWT 调用网关查询接口，并自动解密响应 data。
     * 查询请求不加密请求体，不会修改资金或交易状态；可用于网络异常后的结果确认。
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
     * 该方法会校验订单号、币种、金额、支付方式和支付方式数据，并按最新 OpenAPI 协议提交加密请求。
     * 使用默认 HTTP 传输层时会真实向网关发起代付申请，可能创建测试代付交易并影响测试余额；使用模拟传输层时只返回模拟网关响应。
     * 本方法不保证商户侧幂等，不落库，不做终态保护，不确认渠道最终出款状态；最终结果应结合查询接口或异步通知处理。
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
     * 该方法通过 GET + Bearer JWT 查询平台代付交易，不发送加密请求体。
     * 查询不会修改资金或订单状态，适合在代付申请、取消或回调异常后确认网关侧当前状态。
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
     * 该方法使用 tradeNo 或 orderNo 作为 JWT jti 依据，按最新协议加密请求并提交到代付取消接口。
     * 取消请求可能改变网关侧代付状态；SDK 不负责商户本地事务、状态终态保护或取消后的资金对账。
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
     * 该方法会校验原交易号、币种、原金额、退款金额和退款原因，并提交加密退款请求。
     * 退款涉及资金状态变化；SDK 只负责协议封装和响应解密，不负责商户侧退款幂等、余额处理、清结算或对账。
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
     * 该方法通过 GET + Bearer JWT 查询退款结果，不发送加密请求体，不修改资金或交易状态。
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
     * 该方法调用资金余额检索接口并解密响应 data；请求本身不包含资金变更指令，不修改余额、冻结金额或提现金额。
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
     * 该方法把 currency 放入 query string，并通过 GET + Bearer JWT 调用余额检索接口。
     * 查询不会修改资金数据；返回金额以网关响应为准，商户展示或对账时应按币种精度处理。
     *
     * @param currency 币种，通常为 ISO 4217 三位大写代码
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
     * 该方法校验客户姓名、邮箱和国家后提交加密请求。
     * 请求涉及客户个人信息，SDK 只负责加密传输和日志脱敏，不负责商户侧客户资料合规存储或外部 KYC 流程。
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
     * 该方法通过 GET + Bearer JWT 查询客户资料，不发送加密请求体，不修改客户状态或交易状态。
     * 返回数据可能包含个人信息，商户日志和展示层应继续按自身合规要求脱敏。
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
     * 该方法是支付、代付、退款、客户创建等有请求体接口的统一入口，会先加密明文业务对象，再执行 HTTP 调用和响应解密。
     * 方法本身不开启数据库事务，不做商户侧幂等控制；如果底层传输层是真实 HTTP，请求可能已经到达网关。
     *
     * @param api API 元数据，包含接口名称、HTTP 方法和路径
     * @param request 明文请求对象，可能包含金额、客户资料、卡信息等敏感数据
     * @param responseType 响应 data 类型
     * @param jwtId JWT jti，参与防重放，建议与商户订单号或业务标识关联
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

    /**
     * 创建代收交易的统一内部入口。
     *
     * 收银台、本地支付和信用卡直连最终都会进入该方法，统一完成基础参数校验和加密 POST 调用。
     * 本方法可能真实创建网关支付交易，取决于 HTTP 传输层；不负责商户侧幂等、入账、风控或状态流转。
     *
     * @param request 代收创建请求
     * @return 代收交易响应
     */
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
        OpenApiPayloadParts encryptedParts = payloadCrypto.encryptToParts(requestJson, platformPublicKey);
        return OpenApiEncryptedRequest.builder()
                .livemode(config.getLivemode())
                .data(encryptedParts.toCompactPayload())
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

    /**
     * 记录 OpenAPI 调用开始摘要。
     *
     * 该日志用于商户核对 apiName、method、path、merchantId 和 requestId，不包含请求明文或完整密钥材料。
     *
     * @param api API 元数据
     * @param path 请求路径
     * @param requestId SDK 本地链路请求 ID
     */
    private void logApiStart(OpenApiEndpoint api, String path, String requestId) {
        logJson("API调用开始", logFields(
                "apiName", api.getApiName(),
                "method", api.getMethod(),
                "path", path,
                "merchantId", config.getMerchantId(),
                "requestId", requestId));
    }

    /**
     * 记录 OpenAPI 调用结束摘要。
     *
     * 该日志只输出接口名、路径、状态码、耗时和成功标记，不输出明文卡号或完整鉴权 Token。
     *
     * @param api API 元数据
     * @param path 请求路径
     * @param requestId SDK 本地链路请求 ID
     * @param statusCode HTTP 状态码
     * @param startMillis 请求开始毫秒时间
     * @param success HTTP 层是否成功
     */
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

    /**
     * 记录请求原始明文报文。
     *
     * 该日志只在 rawHttpLogEnabled=true 时输出，并通过 OpenApiLogSanitizer 脱敏卡号、CVC、手机号、邮箱等敏感字段。
     * 生产环境建议谨慎开启；本方法不参与签名或加密计算，不修改业务状态。
     *
     * @param plainRequest 原始业务请求对象
     */
    private void logPlainRequest(Object plainRequest) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || plainRequest == null) {
            return;
        }
        logJson("请求原始明文报文", OpenApiLogSanitizer.sanitizeObject(plainRequest));
    }

    /**
     * 记录 SDK 真实发送给网关的加密请求参数。
     *
     * rawHttpLogEnabled=true 时输出完整 livemode 和 data；关闭时只输出密文摘要，便于生产环境排查但不泄露完整报文。
     *
     * @param encryptedRequest OpenAPI 加密请求外壳
     */
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

    /**
     * 记录请求地址。
     *
     * 该日志仅在 rawHttpLogEnabled=true 时输出完整 URI，便于商户核验 base-url、path 和 query string。
     *
     * @param requestUri SDK 真实准备访问的 URI
     */
    private void logRequestAddress(URI requestUri) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            return;
        }
        log.info("请求地址: {}", requestUri.toString());
        log.debug("请求地址: {}", requestUri.toString());
    }

    /**
     * 记录网关原始密文响应。
     *
     * 该日志只在 rawHttpLogEnabled=true 时输出，用于商户对照平台返回的 code、msg、livemode 和密文 data。
     * 响应 Header 会经过脱敏处理；本方法不解密 data，不修改交易或资金状态。
     *
     * @param response HTTP 原始响应
     * @param encryptedResponse 网关响应外壳
     */
    private void logEncryptedResponse(SdkHttpResponse response, OpenApiEncryptedResponse encryptedResponse) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled())) {
            return;
        }
        log.info("响应原始密文参数：{}" , JsonSupport.toJson(encryptedResponse));
        logJson("响应原始密文参数", logFields(
                "statusCode", response.getStatusCode(),
                "headers", OpenApiLogSanitizer.sanitizeHeaders(response.getHeaders()),
                "body", encryptedResponse));
    }

    /**
     * 记录响应原始明文参数。
     *
     * 该日志只在响应 data 解密并反序列化成功后输出，且会经过敏感字段脱敏。
     * 方法不做商户侧状态更新，不能替代商户自己的订单落库、幂等判断或对账。
     *
     * @param responseData 响应 data 明文对象
     */
    private void logPlainResponse(Object responseData) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || responseData == null) {
            return;
        }
        logJson("响应原始明文参数", OpenApiLogSanitizer.sanitizeObject(responseData));
    }

    /**
     * 记录请求 compact payload 拆分结果。
     *
     * 输出 protectedHeader、header、encryptedAesKey、iv、cipherText、tag，方便商户编写文档或排查加密流程。
     * 仅在 rawHttpLogEnabled=true 时输出；这些字段属于加密调试材料，不参与业务状态处理。
     *
     * @param encryptedRequest OpenAPI 加密请求外壳
     */
    private void logRequestPayloadComponents(OpenApiEncryptedRequest encryptedRequest) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || encryptedRequest == null) {
            return;
        }
        logEncryptedPayloadComponents("请求参数拆分", encryptedRequest.getData());
    }

    /**
     * 记录响应 compact payload 拆分结果。
     *
     * 该方法只拆分网关返回的密文 data，便于核验响应加密结构；不会解密 AES 会话密钥或修改交易状态。
     *
     * @param encryptedResponse 网关响应外壳
     */
    private void logResponsePayloadComponents(OpenApiEncryptedResponse encryptedResponse) {
        if (!Boolean.TRUE.equals(config.getRawHttpLogEnabled()) || encryptedResponse == null) {
            return;
        }
        logEncryptedPayloadComponents("响应参数拆分", encryptedResponse.getData());
    }

    /**
     * 拆分并记录 compact payload 的五段结构。
     *
     * 如果 payload 为空或格式不是五段 compact 结构，会记录跳过原因，不抛出异常影响主调用链。
     *
     * @param logName 日志名称
     * @param compactPayload JWE compact payload
     */
    private void logEncryptedPayloadComponents(String logName, String compactPayload) {
        OpenApiPayloadParts parts = compactPayloadComponentsForLog(compactPayload);
        if (parts == null) {
            logJson(logName + "跳过", logFields("reason", "invalid_compact_payload"));
            return;
        }
        logJson(logName, logFields(
                "protectedHeader", parts.getProtectedHeader(),
                "header", parts.getHeader(),
                "encryptedAesKey", parts.getEncryptedAesKey(),
                "iv", parts.getIv(),
                "cipherText", parts.getCipherText(),
                "tag", parts.getTag()));
    }

    /**
     * 为日志拆分 compact payload。
     *
     * 该方法只用于调试日志，不参与真实解密和业务结果判断；格式不合法时返回 null。
     *
     * @param compactPayload JWE compact payload
     * @return payload 五段结构，格式错误时返回 null
     */
    static OpenApiPayloadParts compactPayloadComponentsForLog(String compactPayload) {
        if (StringUtils.isBlank(compactPayload)) {
            return null;
        }
        try {
            return new OpenApiPayloadCrypto().splitCompactPayload(compactPayload);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 将单对象响应转换为商户可读 SDK 结果。
     *
     * 该方法会校验 livemode、复制 code/msg、解密 data 并反序列化为目标响应类型。
     * 解密成功只代表网关响应可读，不代表支付、代付或退款业务一定成功；商户应继续检查 code 和 data.status。
     *
     * @param api API 元数据
     * @param encryptedResponse 网关加密响应外壳
     * @param responseType 响应 data 目标类型
     * @param requestId SDK 本地链路请求 ID
     * @param <T> 响应 data 类型
     * @return SDK 通用响应对象
     */
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

    /**
     * 将列表响应转换为商户可读 SDK 结果。
     *
     * 该方法兼容网关 data 返回数组或单对象两种形态：数组按列表解析，单对象包装为单元素列表。
     * 方法不修改资金或业务状态，金额精度和币种含义以网关响应字段为准。
     *
     * @param api API 元数据
     * @param encryptedResponse 网关加密响应外壳
     * @param elementType 列表元素类型
     * @param requestId SDK 本地链路请求 ID
     * @param <T> 列表元素类型
     * @return SDK 列表响应
     */
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

    /**
     * 校验响应 livemode 与本地配置是否一致。
     *
     * 该校验用于避免测试环境和生产环境响应混用；不修改资金、订单状态或密钥配置。
     *
     * @param responseLivemode 网关响应 livemode
     */
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

    /**
     * 以统一格式输出 info 和 debug 日志。
     *
     * 该方法会使用 JsonSupport.toLogJson 忽略 null 字段，便于商户阅读请求地址、Header、明文、密文和拆分参数。
     *
     * @param name 日志名称
     * @param value 日志对象
     */
    private void logJson(String name, Object value) {
        String json = JsonSupport.toLogJson(value);
        log.info("{}: {}", name, json);
        log.debug("{}: {}", name, json);
    }

    /**
     * 构造有序日志字段。
     *
     * 该方法仅用于日志输出，不参与签名、加密、资金计算或状态流转。
     *
     * @param keyValues key/value 交替传入的字段
     * @return 有序日志 Map
     */
    private static Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }

    /**
     * 拼接 OpenAPI 请求地址。
     *
     * 该方法只把配置中的 baseUri 与接口 path 合并，不做网络访问、不参与签名或加密。
     *
     * @param path 接口路径，可包含 query string
     * @return 完整请求 URI
     */
    private URI resolveUri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(config.getBaseUri() + normalizedPath);
    }

    /**
     * 校验代收创建请求的最小必填字段。
     *
     * 该校验只覆盖 SDK 发起请求所需的基础字段，不替代网关侧业务校验、风控校验或支付方式参数校验。
     *
     * @param request 代收创建请求
     */
    private void validatePaymentCreateRequest(PaymentCreateRequest request) {
        requireObject(request, "payment request");
        requireText(request.getOrderNo(), "orderNo");
        requireText(request.getCurrency(), "currency");
        requireObject(request.getAmount(), "amount");
    }

    /**
     * 校验代付创建请求的最小必填字段。
     *
     * 该校验确保 SDK 能构造加密请求；余额、风控、支付方式数据完整性和渠道限制由网关继续校验。
     *
     * @param request 代付创建请求
     */
    private void validatePayoutCreateRequest(PayoutCreateRequest request) {
        requireObject(request, "payout request");
        requireText(request.getOrderNo(), "orderNo");
        requireText(request.getCurrency(), "currency");
        requireObject(request.getAmount(), "amount");
        requireText(request.getPaymentMethod(), "paymentMethod");
        requireObject(request.getPaymentMethodData(), "paymentMethodData");
    }

    /**
     * 校验退款创建请求的最小必填字段。
     *
     * 该校验不计算可退金额、不判断原交易状态、不处理退款幂等；这些资金和状态规则由网关及商户业务系统负责。
     *
     * @param request 退款创建请求
     */
    private void validateRefundCreateRequest(RefundCreateRequest request) {
        requireObject(request, "refund request");
        requireText(request.getTradeNo(), "tradeNo");
        requireText(request.getCurrency(), "currency");
        requireObject(request.getAmount(), "amount");
        requireObject(request.getRefundAmount(), "refundAmount");
        requireText(request.getRefundReason(), "refundReason");
    }

    /**
     * 校验客户创建请求的最小必填字段。
     *
     * 该校验只确认 SDK 请求可构造，不替代商户侧客户资料合规校验或网关侧业务校验。
     *
     * @param request 客户创建请求
     */
    private void validateCustomerCreateRequest(CustomerCreateRequest request) {
        requireObject(request, "customer request");
        requireText(request.getFirstname(), "firstname");
        requireText(request.getLastname(), "lastname");
        requireText(request.getEmail(), "email");
        requireText(request.getCountry(), "country");
    }

    /**
     * 校验对象字段不能为空。
     *
     * @param value 字段值
     * @param fieldName 字段名
     * @param <T> 字段类型
     * @return 原字段值
     */
    private static <T> T requireObject(T value, String fieldName) {
        if (value == null) {
            throw new OpenApiValidationException(fieldName + " can not be null");
        }
        return value;
    }

    /**
     * 校验文本字段不能为空白。
     *
     * @param value 字段值
     * @param fieldName 字段名
     * @return 去除首尾空白后的字段值
     */
    private static String requireText(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new OpenApiValidationException(fieldName + " can not be blank");
        }
        return value.trim();
    }

    /**
     * 编码 path 变量。
     *
     * 该方法用于 tradeNo、refundNo、customerId 等路径参数，避免特殊字符破坏 URL。
     *
     * @param value 原始路径参数
     * @return URL path 安全的编码结果
     */
    private static String encodePath(String value) {
        return encodeQuery(value).replace("+", "%20");
    }

    /**
     * 编码 query 参数。
     *
     * @param value 原始 query 参数
     * @return UTF-8 URL 编码结果
     */
    private static String encodeQuery(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception exception) {
            throw new OpenApiValidationException("value can not be encoded", exception);
        }
    }
}
