package com.wikerx.payment.gateway.sdk.config;

/**
 * Payment Gateway SDK 内部固定协议常量。
 */
public final class PaymentGatewayConstants {

    private PaymentGatewayConstants() {
    }

    /**
     * SDK 默认读取的商户配置文件名，文件应放在商户服务端 classpath 下。
     */
    public static final String CONFIG_FILE_NAME = "merchant-config.properties";

    /**
     * 保留默认版本字段，方便兼容商户配置；当前路径不再拼接版本。
     */
    public static final String DEFAULT_VERSION = "v1";

    /**
     * JWT 默认有效期，单位秒。后端防重放窗口当前最大为 180 秒。
     */
    public static final int JWT_TTL_SECONDS = 180;

    /**
     * HTTP 默认连接超时时间，单位毫秒。
     */
    public static final int HTTP_CONNECT_TIMEOUT_MS = 3000;

    /**
     * HTTP 默认响应读取超时时间，单位毫秒。
     */
    public static final int HTTP_READ_TIMEOUT_MS = 10000;

    /**
     * SDK 主请求 Header Map 容量。
     */
    public static final int HTTP_HEADER_MAP_SIZE = 8;

    /**
     * SDK 响应 Header Map 容量。
     */
    public static final int HTTP_RESPONSE_HEADER_MAP_SIZE = 8;

    /**
     * 测试或响应外壳 Map 容量。
     */
    public static final int RESPONSE_BODY_MAP_SIZE = 4;

    /**
     * HTTP 2xx 成功状态码下界。
     */
    public static final int HTTP_STATUS_SUCCESS_MIN = 200;

    /**
     * HTTP 2xx 成功状态码上界，300 本身不属于成功区间。
     */
    public static final int HTTP_STATUS_SUCCESS_MAX_EXCLUSIVE = 300;

    /**
     * OpenAPI 请求内容类型，固定使用 UTF-8 JSON。
     */
    public static final String CONTENT_TYPE = "application/json; charset=UTF-8";

    /**
     * OpenAPI 响应类型，SDK 只接收 JSON 响应。
     */
    public static final String ACCEPT = "application/json";

    /**
     * Payment Gateway 成功响应码。
     */
    public static final int RESPONSE_CODE_SUCCESS = 0;

    /**
     * JWT Authorization Header 前缀。
     */
    public static final String AUTHORIZATION_PREFIX = "Bearer ";

    /**
     * 历史 Payment Authorization Header 前缀，未标注新协议的接口仍要求该鉴权方式。
     */
    public static final String AUTHORIZATION_PAYMENT_PREFIX = "Payment ";

    /**
     * Authorization Header 名称。
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Content-Type Header 名称。
     */
    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * Accept Header 名称。
     */
    public static final String HEADER_ACCEPT = "Accept";

    /**
     * User-Agent Header 名称。
     */
    public static final String HEADER_USER_AGENT = "User-Agent";

    /**
     * SDK 链路请求 ID Header 名称。
     */
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    /**
     * compact 加密 Header 的 typ 字段名。
     */
    public static final String PAYLOAD_HEADER_TYPE = "typ";

    /**
     * compact 加密 Header 的 alg 字段名。
     */
    public static final String PAYLOAD_HEADER_ALGORITHM = "alg";

    /**
     * compact 加密 Header 的 enc 字段名。
     */
    public static final String PAYLOAD_HEADER_ENCRYPTION = "enc";

    /**
     * JWT typ Header 字段名。
     */
    public static final String JWT_HEADER_TYPE = "typ";

    /**
     * JWT typ Header 固定值。
     */
    public static final String JWT_TYPE = "JWT";

    /**
     * SDK 名称，用于 User-Agent。
     */
    public static final String SDK_NAME = "payment-gateway-java-sdk";

    /**
     * SDK 版本，用于 User-Agent。
     */
    public static final String SDK_VERSION = "0.1.0-SNAPSHOT";

    /**
     * SDK 默认 User-Agent。
     */
    public static final String USER_AGENT = SDK_NAME + "/" + SDK_VERSION + " java/1.8";

    /**
     * 加密 compact header 中的类型字段。
     * <p>
     * 服务端当前固定校验 `PAYMENT-PAYLOAD`。
     */
    public static final String PAYLOAD_TYPE = "PAYMENT-PAYLOAD";

    /**
     * 代收创建接口路径。
     */
    public static final String PAYMENT_CREATE_PATH = "/pay-api/trade/payment";

    /**
     * 代收查询接口路径模板。
     */
    public static final String PAYMENT_RETRIEVE_PATH = "/pay-api/trade/payment/%s";

    /**
     * 退款创建接口路径。
     */
    public static final String REFUND_CREATE_PATH = "/pay-api/trade/refund";

    /**
     * 退款查询接口路径模板。
     */
    public static final String REFUND_RETRIEVE_PATH = "/pay-api/trade/refund/%s";

    /**
     * 代付创建接口路径。
     */
    public static final String PAYOUT_CREATE_PATH = "/pay-api/payout/trade/transfer";

    /**
     * 代付查询接口路径模板。
     */
    public static final String PAYOUT_RETRIEVE_PATH = "/pay-api/payout/trade/transfer/%s";

    /**
     * 代付取消接口路径。
     */
    public static final String PAYOUT_CANCEL_PATH = "/pay-api/payout/trade/transfer-cancel";

    /**
     * 余额查询接口路径。
     */
    public static final String BALANCE_RETRIEVE_PATH = "/pay-api/fund/accounts/get";

    /**
     * 客户创建接口路径。
     */
    public static final String CUSTOMER_CREATE_PATH = "/pay-api/mer/customers";

    /**
     * 客户查询接口路径模板。
     */
    public static final String CUSTOMER_RETRIEVE_PATH = "/pay-api/mer/customers/%s";
}
