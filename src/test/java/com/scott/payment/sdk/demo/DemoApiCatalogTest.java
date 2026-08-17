package com.scott.payment.sdk.demo;

import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.model.common.ProductInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demo API 目录支付方式配置测试。
 *
 * 本测试只读取页面元数据，不启动 Spring Boot、不访问真实支付网关，也不会创建交易。
 */
class DemoApiCatalogTest {

    /**
     * 验证收银台和直连代收页面均展示仅支持代收的新增支付方式。
     */
    @Test
    void payinPages_shouldExposeAllNewPaymentMethods() {
        assertThat(optionValues(field("payin-checkout", "paymentMethodTypes")))
                .contains("BTC_ON_CHAIN", "BTC_LIGHT_NETWORK", "PYUSD", "GOOGLE_OR_APPLE");

        assertThat(optionValues(field("payin-direct", "paymentMethod")))
                .contains("BTC_ON_CHAIN", "BTC_LIGHT_NETWORK", "PYUSD", "GOOGLE_OR_APPLE")
                .doesNotHaveDuplicates();
    }

    /**
     * 验证代付页面只展示支持代付的两个新增方式，并提供收款地址字段。
     */
    @Test
    void payoutPage_shouldExposeSupportedCryptoMethodsAndAddress() {
        assertThat(optionValues(field("payout-create", "paymentMethod")))
                .contains("BTC_ON_CHAIN", "PYUSD")
                .doesNotContain("BTC_LIGHT_NETWORK", "GOOGLE_OR_APPLE");

        DemoApiField address = field("payout-create", "address");
        assertThat(address.getDescription()).contains("BTC_ON_CHAIN", "PYUSD");
    }

    /**
     * 验证两个代收创建页面都展示必填商品列表，并提供符合对外文档结构的可编辑默认值。
     */
    @Test
    void payinCreatePages_shouldExposeRequiredProductJson() {
        assertProductField("payin-checkout");
        assertProductField("payin-direct");
    }

    private void assertProductField(String apiCode) {
        DemoApiField productField = field(apiCode, "product");
        assertThat(productField.isRequired()).isTrue();
        assertThat(productField.isTextarea()).isTrue();

        List<ProductInfo> products = JsonSupport.fromJsonList(productField.getDefaultValue(), ProductInfo.class);
        assertThat(products).hasSize(1);
        assertThat(JsonSupport.toJson(products.get(0)))
                .contains("\"name\"", "\"description\"", "\"sku\"", "\"quantity\"", "\"price\"", "\"url\"");
    }

    private DemoApiField field(String apiCode, String fieldName) {
        for (DemoApiField field : DemoApiCatalog.getRequired(apiCode).getRequestFields()) {
            if (fieldName.equals(field.getName())) {
                return field;
            }
        }
        throw new AssertionError("Demo field not found: " + apiCode + "." + fieldName);
    }

    private List<String> optionValues(DemoApiField field) {
        List<String> values = new ArrayList<String>();
        for (DemoApiField.Option option : field.getOptions()) {
            values.add(option.getValue());
        }
        return values;
    }
}
