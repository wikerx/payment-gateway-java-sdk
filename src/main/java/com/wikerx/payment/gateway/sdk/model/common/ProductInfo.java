package com.wikerx.payment.gateway.sdk.model.common;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 商品或服务资料。
 */
@Data
public class ProductInfo {

    /**
     * 商品或服务 ID。
     */
    private String productId;
    /**
     * 姓名。
     */
    private String name;
    /**
     * 商品或服务描述。
     */
    private String description;
    /**
     * 交易或账户币种，使用 ISO 4217 三位大写币种代码。
     */
    private String currency;
    /**
     * 金额，使用 BigDecimal 表示主币种单位。
     */
    private BigDecimal amount;
    /**
     * 数量。
     */
    private Integer quantity;
    /**
     * 商户透传字段。
     */
    private Map<String, Object> metadata;
}
