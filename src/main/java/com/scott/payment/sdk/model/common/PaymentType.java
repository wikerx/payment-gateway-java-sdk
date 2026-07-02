package com.scott.payment.sdk.model.common;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentType
 * @date : 2026-07-02 14:18
 * @email : scott_x@163.com
 * @description : 代收支付类型枚举，负责为商户 SDK 创建代收交易时的 payType 字段提供固定取值。
 *                本枚举只表达网关协议中的支付类型代码，不承载支付方式资料，不执行签名、OpenAPI 报文加密、资金扣款、
 *                幂等处理或交易状态流转；商户应根据 API 文档选择收银台或本地支付直连。
 * @status : create
 */
public enum PaymentType {

    /**
     * 收银台代收，由网关返回跳转地址或客户端密钥，商户前端继续完成支付。
     */
    Checkout(0, "收银台"),

    /**
     * 本地支付直连，商户后端直接提交 paymentMethod 和 paymentMethodData。
     */
    Direct(1, "直连"),

    /**
     * WebSDK
     */
    WebSDK(5, "WebSDK"),

    /**
     * 订阅及循环付款
     */
    Recurring(6, "重复性支付");

    /**
     * 网关 payType 字段使用的数字代码。
     */
    private final Integer code;

    /**
     * 支付类型中文说明，用于商户示例和联调日志。
     */
    private final String description;

    PaymentType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取网关 payType 字段使用的数字代码。
     *
     * @return 支付类型代码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取支付类型中文说明。
     *
     * @return 支付类型中文说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据网关 payType 数字代码解析支付类型枚举。
     *
     * 该方法只做 SDK 本地枚举映射，不访问网关、不修改交易状态、不执行签名或加密。
     *
     * @param code 网关 payType 字段代码
     * @return 匹配的支付类型枚举
     * @throws IllegalArgumentException code 为空或不属于当前 SDK 支持的支付类型时抛出
     */
    public static PaymentType fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("payType code can not be null");
        }
        for (PaymentType paymentType : values()) {
            if (paymentType.code.equals(code)) {
                return paymentType;
            }
        }
        throw new IllegalArgumentException("unsupported payType code: " + code);
    }
}
