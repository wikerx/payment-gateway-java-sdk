package com.scott.payment.sdk.model.payment;

import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.common.DeviceInfo;
import com.scott.payment.sdk.model.common.ProductInfo;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 代收创建请求基础模型。
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
}
