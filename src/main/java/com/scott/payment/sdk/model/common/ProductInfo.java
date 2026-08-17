package com.scott.payment.sdk.model.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 代收订单购买的商品或服务信息。
 *
 * 对外 OpenAPI 要求 {@code name}、{@code description}、{@code sku}、{@code quantity}、{@code price} 和
 * {@code url} 全部必填。数量在 Java 中使用 Integer 便于商户赋值，但会按网关协议序列化为 JSON 字符串；价格直接使用字符串，
 * 避免使用 double/float 产生精度误差。历史字段暂时保留用于源码兼容，新接入请只使用当前文档字段。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductInfo {

    /**
     * 历史商品或服务 ID，新协议请使用 sku。
     */
    @Deprecated
    private String productId;
    /**
     * 商品或服务名称，必填。
     */
    private String name;
    /**
     * 商品或服务详细描述，必填。
     */
    private String description;
    /**
     * SKU 商品 ID，必填。
     */
    private String sku;
    /**
     * 商品数量，必填；发送到网关时序列化为字符串。
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Integer quantity;
    /**
     * 商品单价，必填；使用十进制字符串表示，避免浮点精度问题。
     */
    private String price;
    /**
     * 商品详情链接，必填。
     */
    private String url;
    /**
     * 历史商品币种字段，新协议使用代收请求顶层 currency。
     */
    @Deprecated
    private String currency;
    /**
     * 历史商品金额字段，新协议请使用 price。
     */
    @Deprecated
    private BigDecimal amount;
    /**
     * 历史商品透传字段，新协议未要求该字段。
     */
    @Deprecated
    private Map<String, Object> metadata;
}
