package com.scott.payment.sdk.model.common;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentMethod
 * @date : 2026-07-01 17:43
 * @email : scott_x@163.com
 * @description : OpenAPI 支付方式枚举，负责为商户 SDK 请求中的 paymentMethod 字段提供固定取值，避免商户手写字符串导致参数错误。
 *                本枚举只表达支付方式代码和展示说明，不承载支付方式扩展参数，不执行签名、加密、资金计算、状态流转或外部渠道调用。
 *                枚举 code 必须与网关 API 文档保持一致，新增或废弃支付方式时需要同步更新 SDK 示例和商户文档。
 * @status : create
 */
public enum PaymentMethod {

    /**
     * 信用卡。
     */
    CARD("CARD", "信用卡"),

    /**
     * PayPal。
     */
    PAY_PAL("PAY_PAL", "PayPal"),

    /**
     * Cash App。
     */
    CASHAPP("CASHAPP", "Cash App"),

    /**
     * ACH 直接借记。
     */
    ACH_DEBIT("ACH_DEBIT", "ACH 直接借记"),

    /**
     * UPI。
     */
    UPI("UPI", "印度 UPI");

    /**
     * 网关接口要求的支付方式代码。
     */
    private final String code;

    /**
     * 支付方式中文说明，用于商户示例、日志摘要或文档展示。
     */
    private final String description;

    PaymentMethod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取网关接口 paymentMethod 字段使用的代码。
     *
     * @return 支付方式代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取支付方式说明。
     *
     * @return 支付方式中文说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据支付方式代码解析枚举。
     *
     * 该方法只做本地枚举映射，不访问网关、不修改交易或资金状态；入参会去除首尾空格并忽略大小写。
     *
     * @param code 网关 paymentMethod 字段代码
     * @return 匹配的支付方式枚举
     * @throws IllegalArgumentException code 为空或不属于当前 SDK 支持的支付方式时抛出
     */
    public static PaymentMethod fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("paymentMethod code can not be blank");
        }
        String normalizedCode = code.trim();
        for (PaymentMethod paymentMethod : values()) {
            if (paymentMethod.code.equalsIgnoreCase(normalizedCode)) {
                return paymentMethod;
            }
        }
        throw new IllegalArgumentException("unsupported paymentMethod code: " + code);
    }
}
