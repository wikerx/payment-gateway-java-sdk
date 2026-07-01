package com.scott.payment.sdk.model.payment;

import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.DeviceInfo;
import com.scott.payment.sdk.model.common.PaymentMethod;
import com.scott.payment.sdk.model.common.ProductInfo;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentCreateRequest
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : 代收创建请求基础模型，负责承载商户创建收银台支付或直连支付时提交的订单、金额、客户、设备和支付方式参数。
 *                本类只描述请求字段和本地枚举赋值兼容逻辑，不执行 JWT 签名、OpenAPI 报文加密、HTTP 调用、资金扣款、幂等落库或交易状态流转。
 *                amount 涉及资金金额，paymentMethodData 可能包含卡号、CVC 等敏感数据，商户侧不得在普通日志中直接输出完整对象。
 * @status : modify
 */
@Data
public class PaymentCreateRequest {

    /**
     * 商户订单号。
     */
    private String orderNo;
    /**
     * 支付类型。
     */
    private Integer payType;
    /**
     * 交易或账户币种，使用 ISO 4217 三位大写币种代码。
     */
    private String currency;
    /**
     * 金额，使用 BigDecimal 表示主币种单位。
     */
    private BigDecimal amount;
    /**
     * 支付完成后的前端返回地址。
     */
    private String returnUrl;
    /**
     * 异步通知地址。
     */
    private String notifyUrl;
    /**
     * 客户 ID。
     */
    private String customerId;
    /**
     * 客户资料。
     */
    private CustomerInfo customer;
    /**
     * 设备环境资料。
     */
    private DeviceInfo device;
    /**
     * 客户端 IP。
     */
    private String clientIp;
    /**
     * 商户网站地址。
     */
    private String website;
    /**
     * 商品或服务 ID。
     */
    private String productId;
    /**
     * 商品或服务列表。
     */
    private List<ProductInfo> product;
    /**
     * 订单失效时间，单位秒。
     */
    private Long expiredTime;
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
    private Object paymentMethodData;

    /**
     * 订阅类型。
     */
    private Integer subType;
    /**
     * 订阅周期。
     */
    private Integer subscriptionMode;
    /**
     * 订阅 token。
     */
    private String subToken;
    /**
     * 扩展字段。
     */
    private Map<String, Object> extra;

    /**
     * 使用原始字符串设置 paymentMethod。
     *
     * 该方法用于兼容历史商户代码和网关可能先于 SDK 发布的新支付方式，只做字段赋值，不做枚举校验。
     * 本方法不执行签名、加密、资金计算或状态流转；调用方应保证字符串取值符合网关 API 文档。
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
     * 本方法不执行签名、加密、资金计算或状态流转。
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
