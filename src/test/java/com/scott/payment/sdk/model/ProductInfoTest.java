package com.scott.payment.sdk.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.model.common.ProductInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品请求模型 JSON 协议测试。
 *
 * 本测试不访问支付网关，仅验证 Java 模型生成的商品对象与对外 OpenAPI 文档保持一致。
 */
class ProductInfoTest {

    /**
     * 验证商品模型完整保留六个必填字符串字段，尤其避免数量和单价被序列化为 JSON 数字。
     */
    @Test
    void productInfo_shouldSerializeDocumentedFieldsAsStrings() {
        ProductInfo product = JsonSupport.fromJson("{"
                + "\"name\":\"SDK Demo Product\","
                + "\"description\":\"Payment Gateway Java SDK test product\","
                + "\"sku\":\"SKU_10001\","
                + "\"quantity\":\"1\","
                + "\"price\":\"12.34\","
                + "\"url\":\"https://merchant.example.com/products/SKU_10001\""
                + "}", ProductInfo.class);

        JsonNode json = JsonSupport.objectMapper().valueToTree(product);

        assertThat(json.size()).isEqualTo(6);
        assertThat(json.get("name").asText()).isEqualTo("SDK Demo Product");
        assertThat(json.get("description").asText()).isEqualTo("Payment Gateway Java SDK test product");
        assertThat(json.get("sku").asText()).isEqualTo("SKU_10001");
        assertThat(json.get("quantity").isTextual()).isTrue();
        assertThat(json.get("quantity").asText()).isEqualTo("1");
        assertThat(json.get("price").isTextual()).isTrue();
        assertThat(json.get("price").asText()).isEqualTo("12.34");
        assertThat(json.get("url").asText()).isEqualTo("https://merchant.example.com/products/SKU_10001");
    }
}
