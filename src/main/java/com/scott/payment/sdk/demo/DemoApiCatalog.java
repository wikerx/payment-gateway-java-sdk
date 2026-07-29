package com.scott.payment.sdk.demo;

import com.scott.payment.sdk.api.OpenApiEndpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI demo catalog used by Thymeleaf pages.
 */
public final class DemoApiCatalog {

    private static final List<DemoApiGroup> GROUPS = buildGroups();
    private static final Map<String, DemoApiDefinition> API_BY_CODE = buildIndex(GROUPS);

    private DemoApiCatalog() {
    }

    public static List<DemoApiGroup> groups() {
        return GROUPS;
    }

    public static DemoApiDefinition getRequired(String apiCode) {
        DemoApiDefinition definition = API_BY_CODE.get(apiCode);
        if (definition == null) {
            throw new IllegalArgumentException("Unsupported demo api: " + apiCode);
        }
        return definition;
    }

    public static boolean contains(String apiCode) {
        return API_BY_CODE.containsKey(apiCode);
    }

    private static List<DemoApiGroup> buildGroups() {
        List<DemoApiGroup> groups = new ArrayList<DemoApiGroup>();
        groups.add(new DemoApiGroup("customer", "客户", "创建、查询、更新、删除和列出网关客户资料。", Arrays.asList(
                customerCreate(),
                customerRetrieve(),
                customerUpdate(),
                customerDelete(),
                customerList())));
        groups.add(new DemoApiGroup("payin", "代收", "创建收银台或直连代收交易，并查询代收结果。", Arrays.asList(
                payinCheckout(),
                payinDirect(),
                payinRetrieve())));
        groups.add(new DemoApiGroup("refund", "退款申请", "基于代收交易发起退款申请，并查询退款处理结果。", Arrays.asList(
                refundCreate(),
                refundRetrieve())));
        groups.add(new DemoApiGroup("payout", "代付", "发起代付、查询代付和提交取消申请。", Arrays.asList(
                payoutCreate(),
                payoutRetrieve(),
                payoutCancel())));
        groups.add(new DemoApiGroup("balance", "余额查询", "查询商户资金账户余额，可按币种过滤。", Arrays.asList(
                balanceRetrieve())));
        return groups;
    }

    private static Map<String, DemoApiDefinition> buildIndex(List<DemoApiGroup> groups) {
        Map<String, DemoApiDefinition> index = new LinkedHashMap<String, DemoApiDefinition>();
        for (DemoApiGroup group : groups) {
            for (DemoApiDefinition api : group.getApis()) {
                index.put(api.getCode(), api);
            }
        }
        return index;
    }

    private static DemoApiDefinition payinCheckout() {
        return api("payin-checkout", "payin", "代收", "创建收银台代收",
                "创建一笔跳转式代收交易，响应通常包含 tradeNo、redirectUrl 或 clientSecret。",
                "创建支付", OpenApiEndpoint.PAYMENT_CREATE,
                fields(
                        merchantNo(),
                        field("orderNo", "商户订单号", "商户侧唯一订单号，Demo 会自动生成。", true, "AUTO:PAYIN_CHECKOUT_", ""),
                        currencySelect(),
                        field("amount", "金额", "主币种金额，避免使用浮点数。", true, "12.34", "12.34"),
                        field("returnUrl", "返回地址", "支付完成后的前端跳转地址。", false, DemoLocalUrls.PAYIN_RETURN_URL, ""),
                        field("notifyUrl", "异步通知地址", "网关支付结果通知地址。", false, DemoLocalUrls.PAYIN_NOTIFY_URL, ""),
                        field("clientIp", "客户端 IP", "付款人客户端 IP。", false, "47.125.221.223", ""),
                        field("website", "商户网站", "商户站点或业务来源。", false, "https://manage.forgottenthrone.com/", ""),
                        field("metadata", "透传字段", "商户自定义透传数据。", false, "metadata", ""),
                        paymentMethodTypes(),
                        customerMode(),
                        customerId(),
                        customerJson()),
                paymentResponseFields());
    }

    private static DemoApiDefinition payinDirect() {
        return api("payin-direct", "payin", "代收", "创建直连代收",
                "创建一笔直连代收交易，Demo 默认使用 CASHAPP，可修改 paymentMethodData。",
                "发起支付", OpenApiEndpoint.PAYMENT_CREATE,
                fields(
                        merchantNo(),
                        field("orderNo", "商户订单号", "商户侧唯一订单号，Demo 会自动生成。", true, "AUTO:PAYIN_CASHAPP_", ""),
                        select("payType", "支付类型", "直连代收固定为 1。", true, "1", "", options(
                                option("1", "1 - 直连"))),
                        currencySelect(),
                        field("amount", "金额", "主币种金额，避免使用浮点数。", true, "12.34", "12.34"),
                        select("paymentMethod", "支付方式", "支持 CARD、CASHAPP 等网关支付方式。", true, "CASHAPP", "", options(
                                option("CASHAPP", "CASHAPP - Cash App"),
                                option("CARD", "CARD - 信用卡"),
                                option("CHECKOUT", "CHECKOUT - 信用卡收银台"),
                                option("APPLE_PAY", "APPLE_PAY - Apple Pay"),
                                option("GOOGLE_PAY", "GOOGLE_PAY - Google Pay"),
                                option("PAY_PAL", "PAY_PAL - PayPal"),
                                option("ACH_DEBIT", "ACH_DEBIT - ACH 直接借记"),
                                option("UPI", "UPI - 印度 UPI"),
                                option("BTC_ON_CHAIN", "BTC_ON_CHAIN - 比特币链上支付"),
                                option("BTC_LIGHT_NETWORK", "BTC_LIGHT_NETWORK - 比特币轻网络支付"),
                                option("PYUSD", "PYUSD - PayPal USD 稳定币支付"))),
                        json("paymentMethodData", "支付方式参数", "不同支付方式需要不同扩展参数，包含敏感信息时不要输出到生产日志。", true,
                                "{\n  \"cashappAccount\": \"$123\",\n  \"email\": \"lily_brown_1782457030419@test.com\"\n}", ""),
                        field("returnUrl", "返回地址", "支付完成后的前端跳转地址。", false, DemoLocalUrls.PAYIN_RETURN_URL, ""),
                        field("notifyUrl", "异步通知地址", "网关支付结果通知地址。", false, DemoLocalUrls.PAYIN_NOTIFY_URL, ""),
                        field("clientIp", "客户端 IP", "付款人客户端 IP。", false, "47.125.221.223", ""),
                        field("website", "商户网站", "商户站点或业务来源。", false, "http://192.168.2.114:5173", ""),
                        field("metadata", "透传字段", "商户自定义透传数据。", false, "metadata", ""),
                        customerMode(),
                        customerId(),
                        customerJson()),
                paymentResponseFields());
    }

    private static DemoApiDefinition payinRetrieve() {
        return api("payin-retrieve", "payin", "代收", "查询代收交易",
                "通过平台代收交易号查询支付结果，不会创建新交易。",
                "查询支付", OpenApiEndpoint.PAYMENT_RETRIEVE,
                fields(
                        merchantNo(),
                        field("tradeNo", "平台交易号", "创建代收返回的 tradeNo。", true, "pay_202607021541448605052", "pay_xxx")),
                paymentResponseFields());
    }

    private static DemoApiDefinition refundCreate() {
        return api("refund-create", "refund", "退款申请", "创建退款申请",
                "基于代收交易号提交退款申请，原交易不可退时网关会返回业务失败。",
                "申请退款", OpenApiEndpoint.REFUND_CREATE,
                fields(
                        merchantNo(),
                        field("tradeNo", "原代收交易号", "需要退款的原代收 tradeNo。", true, "pay_202607021541448605052", "pay_xxx"),
                        field("currency", "币种", "必须与原交易币种一致。", true, "USD", "USD"),
                        field("amount", "原交易金额", "原代收交易金额。", true, "12.34", "12.34"),
                        field("refundAmount", "退款金额", "本次申请退款金额。", true, "1.00", "1.00"),
                        field("refundReason", "退款原因", "提交给网关的退款原因。", true, "SDK Demo refund request", ""),
                        field("metadata", "透传字段", "商户自定义透传数据。", false, "metadata", ""),
                        field("remark", "备注", "联调备注。", false, "SDK Demo refund", "")),
                refundResponseFields());
    }

    private static DemoApiDefinition refundRetrieve() {
        return api("refund-retrieve", "refund", "退款申请", "查询退款",
                "通过退款标识查询退款处理结果。",
                "查询退款", OpenApiEndpoint.REFUND_RETRIEVE,
                fields(
                        merchantNo(),
                        field("refundNo", "退款标识", "退款申请返回的 charge/refundNo。", true, "charge_202607021549576341310", "charge_xxx")),
                refundResponseFields());
    }

    private static DemoApiDefinition payoutCreate() {
        return api("payout-create", "payout", "代付", "发起代付",
                "创建一笔代付申请，Demo 默认使用 CARD 收款资料。",
                "发起代付", OpenApiEndpoint.PAYOUT_TRANSFER_CREATE,
                fields(
                        merchantNo(),
                        field("orderNo", "商户订单号", "商户侧唯一代付订单号，Demo 会自动生成。", true, "AUTO:PAYOUT_", ""),
                        currencySelect(),
                        field("amount", "金额", "代付出款金额。", true, "3.11", "3.11"),
                        select("paymentMethod", "支付方式", "收款支付方式；BTC_ON_CHAIN、PYUSD 需要填写收款地址。", true, "CARD", "",
                                payoutPaymentMethodOptions()),
                        field("address", "收款地址", "BTC_ON_CHAIN、PYUSD 代付必填，其他支付方式可留空。请填写商户确认过的真实收款地址。", false, "",
                                "选择加密货币支付方式后填写收款地址"),
                        json("paymentMethodData", "支付方式参数", "收款支付方式扩展数据，可能包含卡号或银行账号。", true,
                                "{\n  \"number\": \"4000056655665556\",\n  \"expMonth\": \"06\",\n  \"expYear\": \"2029\",\n  \"cvc\": \"123\"\n}", ""),
                        field("notifyUrl", "异步通知地址", "网关代付结果通知地址。", false, DemoLocalUrls.PAYOUT_NOTIFY_URL, ""),
                        field("clientIp", "客户端 IP", "操作或客户 IP。", false, "47.125.221.223", ""),
                        field("website", "商户网站", "商户站点或业务来源。", false, "https://manage.forgottenthrone.com/", ""),
                        field("metadata", "透传字段", "商户自定义透传数据。", false, "metadata", ""),
                        customerJson()),
                payoutResponseFields());
    }

    private static DemoApiDefinition payoutRetrieve() {
        return api("payout-retrieve", "payout", "代付", "查询代付",
                "通过平台代付交易号查询代付处理结果。",
                "查询代付", OpenApiEndpoint.PAYOUT_TRANSFER_RETRIEVE,
                fields(
                        merchantNo(),
                        field("tradeNo", "平台代付交易号", "代付申请返回的 tradeNo。", true, "payout_202607021105485695090", "payout_xxx")),
                payoutResponseFields());
    }

    private static DemoApiDefinition payoutCancel() {
        return api("payout-cancel", "payout", "代付", "取消代付",
                "提交代付取消申请，已成功或不可取消的交易可能返回业务失败。",
                "取消代付", OpenApiEndpoint.PAYOUT_TRANSFER_CANCEL,
                fields(
                        merchantNo(),
                        field("tradeNo", "平台代付交易号", "代付申请返回的 tradeNo。", true, "payout_202607021532396969266", "payout_xxx"),
                        field("orderNo", "商户订单号", "代付申请时的商户订单号。", true, "PAYOUT_20260702153239394000", ""),
                        field("remark", "备注", "取消原因或联调备注。", false, "SDK Demo payout cancel", "")),
                payoutResponseFields());
    }

    private static DemoApiDefinition balanceRetrieve() {
        return api("balance-retrieve", "balance", "余额查询", "查询资金账户余额",
                "查询商户余额，可填写 currency 只查询某个币种。",
                "查询余额", OpenApiEndpoint.FUND_ACCOUNTS_BALANCE_INQUIRY,
                fields(
                        merchantNo(),
                        field("currency", "币种", "为空则查询全部币种。", false, "USD", "USD")),
                balanceResponseFields());
    }

    private static DemoApiDefinition customerCreate() {
        return api("customer-create", "customer", "客户", "创建客户",
                "创建一条网关客户资料，邮箱和证件号 Demo 会生成唯一值。",
                "创建客户", OpenApiEndpoint.CUSTOMER_CREATE,
                customerRequestFields(false),
                customerResponseFields());
    }

    private static DemoApiDefinition customerRetrieve() {
        return api("customer-retrieve", "customer", "客户", "查询客户",
                "通过 customerId 查询客户资料。",
                "查询客户", OpenApiEndpoint.CUSTOMER_RETRIEVE,
                fields(
                        merchantNo(),
                        field("customerId", "客户 ID", "创建客户返回的 customerId。", true, "cus_ysNthtSsNGrwje3", "cus_xxx")),
                customerResponseFields());
    }

    private static DemoApiDefinition customerUpdate() {
        List<DemoApiField> fields = new ArrayList<DemoApiField>();
        fields.add(merchantNo());
        fields.add(field("customerId", "客户 ID", "需要更新的 customerId，放在接口路径中。", true, "cus_ysNthtSsNGrwje3", "cus_xxx"));
        fields.addAll(customerFields(true));
        return api("customer-update", "customer", "客户", "更新客户",
                "更新网关客户资料，customerId 使用路径参数，请求体只提交可更新字段。",
                "更新客户", OpenApiEndpoint.CUSTOMER_UPDATE,
                fields,
                customerResponseFields());
    }

    private static DemoApiDefinition customerDelete() {
        return api("customer-delete", "customer", "客户", "删除客户",
                "通过 customerId 删除网关客户资料。",
                "删除客户", OpenApiEndpoint.CUSTOMER_DELETE,
                fields(
                        merchantNo(),
                        field("customerId", "客户 ID", "需要删除的 customerId。", true, "cus_ysNthtSsNGrwje3", "cus_xxx")),
                fields(
                        field("code", "code", "业务响应码，0 表示成功。", false, "", ""),
                        field("msg", "msg", "业务响应说明。", false, "", ""),
                        field("data", "data", "true 表示网关已受理删除。", false, "", "")));
    }

    private static DemoApiDefinition customerList() {
        return api("customer-list", "customer", "客户", "列出客户",
                "查询当前商户下的客户列表。",
                "查询列表", OpenApiEndpoint.CUSTOMER_LIST,
                fields(merchantNo()),
                customerResponseFields());
    }

    private static List<DemoApiField> customerRequestFields(boolean update) {
        List<DemoApiField> fields = new ArrayList<DemoApiField>();
        fields.add(merchantNo());
        fields.addAll(customerFields(update));
        return fields;
    }

    private static List<DemoApiField> customerFields(boolean update) {
        List<DemoApiField> fields = new ArrayList<DemoApiField>();
        fields.add(field("firstname", "名", "客户名。", true, update ? "ABC" : "Lily", ""));
        fields.add(field("lastname", "姓", "客户姓。", true, "Brown", ""));
        fields.add(field("email", "邮箱", "客户邮箱，Demo 会在 AUTO_EMAIL 后缀中生成唯一邮箱。", true,
                update ? "AUTO_EMAIL:abc_brown_" : "AUTO_EMAIL:lily_brown_", ""));
        fields.add(field("phone", "电话", "客户联系电话。", false, update ? "13628173753" : "13628173752", ""));
        fields.add(field("identityType", "证件类型", "证件类型，例如 PASSPORT。", false, "PASSPORT", ""));
        fields.add(field("identityNo", "证件号", "客户证件号，Demo 会自动生成后缀。", false, "AUTO:P", ""));
        fields.add(field("country", "国家", "ISO 3166-1 alpha-2 国家代码。", true, "US", "US"));
        fields.add(field("state", "州/省", "州或省。", false, update ? "NY" : "CA", ""));
        fields.add(field("city", "城市", "城市。", false, update ? "New York" : "Los Angeles", ""));
        fields.add(field("address", "地址", "客户地址。", false, update ? "456 Broadway" : "123 Main St, Apt 4B", ""));
        fields.add(field("zipcode", "邮编", "邮政编码。", false, update ? "10001" : "90001", ""));
        return fields;
    }

    private static DemoApiDefinition api(String code,
                                         String groupCode,
                                         String groupName,
                                         String name,
                                         String description,
                                         String actionLabel,
                                         OpenApiEndpoint endpoint,
                                         List<DemoApiField> requestFields,
                                         List<DemoApiField> responseFields) {
        return new DemoApiDefinition(code, groupCode, groupName, name, description, actionLabel,
                endpoint, requestFields, responseFields);
    }

    private static DemoApiField merchantNo() {
        return field("merchantNo", "商户号", "从 merchant-config.properties 自动读取，只用于页面核对。", false, "", "");
    }

    private static DemoApiField customerJson() {
        return json("customer", "客户资料", "网关要求提供 customerId 或 customer，Demo 默认提交客户资料。", true,
                "{\n  \"firstname\": \"Lily\",\n  \"lastname\": \"Brown\",\n  \"email\": \"lily_brown_1782457030419@test.com\",\n  \"phone\": \"13628173752\",\n  \"country\": \"US\",\n  \"state\": \"CA\",\n  \"city\": \"Los Angeles\",\n  \"address\": \"123 Main St, Apt 4B\",\n  \"zipcode\": \"90001\"\n}", "");
    }

    private static DemoApiField customerId() {
        return field("customerId", "客户 ID", "选择客户 ID 模式时提交，和客户资料二选一。", false, "cus_ysNthtSsNGrwje3", "cus_xxx");
    }

    private static DemoApiField customerMode() {
        return select("customerMode", "客户提交方式", "代收创建时 customerId 和 customer 二选一，切换后页面只提交选中的字段。", true, "customer", "", options(
                option("customer", "使用客户资料 customer"),
                option("customerId", "使用客户 ID customerId")));
    }

    private static DemoApiField paymentMethodTypes() {
        return select("paymentMethodTypes", "可用支付方式", "收银台代收选择一个支付方式，Demo 会提交为 paymentMethodTypes 集合。", false, "CARD", "",
                payinPaymentMethodOptions());
    }

    private static List<DemoApiField.Option> payinPaymentMethodOptions() {
        return options(
                option("CARD", "CARD - 信用卡"),
                option("CASHAPP", "CASHAPP - Cash App"),
                option("PAY_PAL", "PAY_PAL - PayPal"),
                option("VENMO", "VENMO - venmo"),
                option("ACH_DEBIT", "ACH_DEBIT - ACH 直接借记"),
                option("UPI", "UPI - 印度 UPI"),
                option("BTC_ON_CHAIN", "BTC_ON_CHAIN - 比特币链上支付"),
                option("BTC_LIGHT_NETWORK", "BTC_LIGHT_NETWORK - 比特币轻网络支付"),
                option("PYUSD", "PYUSD - PayPal USD 稳定币支付"));
    }

    private static List<DemoApiField.Option> payoutPaymentMethodOptions() {
        return options(
                option("CARD", "CARD - 信用卡"),
                option("CASHAPP", "CASHAPP - Cash App"),
                option("PAY_PAL", "PAY_PAL - PayPal"),
                option("VENMO", "VENMO - venmo"),
                option("ACH_DEBIT", "ACH_DEBIT - ACH 直接借记"),
                option("UPI", "UPI - 印度 UPI"),
                option("BTC_ON_CHAIN", "BTC_ON_CHAIN - 比特币链上支付"),
                option("PYUSD", "PYUSD - PayPal USD 稳定币支付"));
    }

    private static DemoApiField field(String name,
                                      String label,
                                      String description,
                                      boolean required,
                                      String defaultValue,
                                      String placeholder) {
        return DemoApiField.input(name, label, description, required, defaultValue, placeholder);
    }

    private static DemoApiField currencySelect() {
        return select("currency", "币种", "ISO 4217 三位币种代码。", true, "USD", "", options(
                option("USD", "USD - 美元"),
                option("EUR", "EUR - 欧元"),
                option("GBP", "GBP - 英镑"),
                option("CNY", "CNY - 人民币")));
    }

    private static DemoApiField select(String name,
                                       String label,
                                       String description,
                                       boolean required,
                                       String defaultValue,
                                       String placeholder,
                                       List<DemoApiField.Option> options) {
        return DemoApiField.select(name, label, description, required, defaultValue, placeholder, options);
    }

    private static DemoApiField json(String name,
                                     String label,
                                     String description,
                                     boolean required,
                                     String defaultValue,
                                     String placeholder) {
        return DemoApiField.textarea(name, label, description, required, defaultValue, placeholder);
    }

    private static List<DemoApiField> fields(DemoApiField... fields) {
        return new ArrayList<DemoApiField>(Arrays.asList(fields));
    }

    private static List<DemoApiField.Option> options(DemoApiField.Option... options) {
        return new ArrayList<DemoApiField.Option>(Arrays.asList(options));
    }

    private static DemoApiField.Option option(String value, String label) {
        return new DemoApiField.Option(value, label);
    }

    private static List<DemoApiField> paymentResponseFields() {
        return fields(
                field("code", "code", "业务响应码，0 表示成功。", false, "", ""),
                field("msg", "msg", "业务响应说明。", false, "", ""),
                field("data.tradeNo", "tradeNo", "平台代收交易流水号，用于查询和退款。", false, "", ""),
                field("data.orderNo", "orderNo", "商户订单号。", false, "", ""),
                field("data.status", "status", "网关交易状态数字。", false, "", ""),
                field("data.redirectUrl", "redirectUrl", "收银台跳转地址。", false, "", ""),
                field("data.clientSecret", "clientSecret", "客户端继续支付需要的密钥。", false, "", ""));
    }

    private static List<DemoApiField> refundResponseFields() {
        return fields(
                field("code", "code", "业务响应码，0 表示成功。", false, "", ""),
                field("msg", "msg", "业务响应说明。", false, "", ""),
                field("data.charge", "charge", "退款标识，可用于查询退款。", false, "", ""),
                field("data.tradeNo", "tradeNo", "原代收交易号。", false, "", ""),
                field("data.status", "status", "退款状态数字。", false, "", ""),
                field("data.refundAmount", "refundAmount", "退款金额。", false, "", ""));
    }

    private static List<DemoApiField> payoutResponseFields() {
        return fields(
                field("code", "code", "业务响应码，0 表示成功。", false, "", ""),
                field("msg", "msg", "业务响应说明。", false, "", ""),
                field("data.tradeNo", "tradeNo", "平台代付交易流水号。", false, "", ""),
                field("data.orderNo", "orderNo", "商户代付订单号。", false, "", ""),
                field("data.status", "status", "代付状态数字。", false, "", ""),
                field("data.message", "message", "网关代付状态说明。", false, "", ""));
    }

    private static List<DemoApiField> balanceResponseFields() {
        return fields(
                field("code", "code", "业务响应码，0 表示成功。", false, "", ""),
                field("msg", "msg", "业务响应说明。", false, "", ""),
                field("data[].merNo", "merNo", "平台商户号。", false, "", ""),
                field("data[].currency", "currency", "账户币种。", false, "", ""),
                field("data[].balance", "balance", "可用余额。", false, "", ""),
                field("data[].frozenAmounts", "frozenAmounts", "冻结金额。", false, "", ""));
    }

    private static List<DemoApiField> customerResponseFields() {
        return fields(
                field("code", "code", "业务响应码，0 表示成功。", false, "", ""),
                field("msg", "msg", "业务响应说明。", false, "", ""),
                field("data.customerId", "customerId", "网关客户 ID。", false, "", ""),
                field("data.firstname", "firstname", "客户名。", false, "", ""),
                field("data.lastname", "lastname", "客户姓。", false, "", ""),
                field("data.email", "email", "客户邮箱。", false, "", ""));
    }
}
