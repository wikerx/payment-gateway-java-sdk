package com.scott.payment.sdk.model.payout;

import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.PaymentMethod;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutCreateRequest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 代付创建请求模型，负责承载商户发起出款申请时提交的订单、金额、客户资料和收款支付方式参数。
 *                本类只描述请求字段和本地枚举赋值兼容逻辑，不执行 JWT 签名、OpenAPI 报文加密、HTTP 调用、资金扣减、幂等落库、状态流转或渠道出款。
 *                amount 涉及资金金额，paymentMethodData 可能包含卡号、银行账号等敏感收款资料，商户侧不得在普通日志中直接输出完整对象。
 * @status : modify
 */
@Data
public class PayoutCreateRequest {

    /**
     * 商户订单号。
     */
    private String orderNo;
    /**
     * 交易或账户币种，使用 ISO 4217 三位大写币种代码。
     */
    private String currency;
    /**
     * 金额，使用 BigDecimal 表示主币种单位。
     */
    private BigDecimal amount;
    /**
     * 客户端 IP。
     */
    private String clientIp;
    /**
     * 商户网站地址。
     */
    private String website;
    /**
     * 异步通知地址。
     */
    private String notifyUrl;
    /**
     * 关联子商户号。
     */
    private String onBehalfOf;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;
    /**
     * 客户 ID。
     */
    private String customerId;
    /**
     * 客户资料。
     */
    private CustomerInfo customer;
    /**
     * 可用支付方式集合。
     */
    private Set<String> paymentMethodTypes;
    /**
     * 支付方式。
     */
    private String paymentMethod;

    /**
     * 支付方式扩展数据，可能包含敏感信息。
     */
    @ToString.Exclude
    private Map<String, Object> paymentMethodData;

    /**
     * 使用原始字符串设置 paymentMethod。
     *
     * 该方法用于兼容历史商户代码和网关可能先于 SDK 发布的新支付方式，只做字段赋值，不做枚举校验。
     * 本方法不执行签名、加密、资金计算、状态流转或外部渠道调用；调用方应保证字符串取值符合网关 API 文档。
     *
     * @param paymentMethod 网关 paymentMethod 字段代码，允许传入网关文档支持但当前枚举暂未收录的取值
     */
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /**
     * 使用支付方式枚举设置 paymentMethod。
     *
     * 该方法只把枚举 code 写入原有 String 字段，不改变 HTTP 字段名和 JSON 结构，兼容商户继续使用 setPaymentMethod(String)。
     * 本方法不执行签名、加密、资金计算、状态流转或外部渠道调用。
     *
     * @param paymentMethod 支付方式枚举，不允许为空
     */
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("paymentMethod can not be null");
        }
        this.paymentMethod = paymentMethod.getCode();
    }
}
