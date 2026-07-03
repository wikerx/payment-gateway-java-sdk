package com.scott.payment.sdk.demo;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.util.OrderNoGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds default request parameters for demo pages.
 */
public class DemoApiFormFactory {

    private final OpenApiClientConfig config;

    public DemoApiFormFactory(OpenApiClientConfig config) {
        this.config = config;
    }

    public DemoApiForm create(DemoApiDefinition definition) {
        DemoApiForm form = new DemoApiForm();
        Map<String, String> params = new LinkedHashMap<String, String>();
        for (DemoApiField field : definition.getRequestFields()) {
            params.put(field.getName(), resolveDefaultValue(field));
        }
        form.setParams(params);
        return form;
    }

    private String resolveDefaultValue(DemoApiField field) {
        if ("merchantNo".equals(field.getName())) {
            return config.getMerchantId();
        }
        String defaultValue = field.getDefaultValue();
        if (defaultValue == null) {
            return "";
        }
        if (defaultValue.startsWith("AUTO_EMAIL:")) {
            return defaultValue.substring("AUTO_EMAIL:".length())
                    + OrderNoGenerator.generate("") + "@test.com";
        }
        if (defaultValue.startsWith("AUTO:")) {
            return OrderNoGenerator.generate(defaultValue.substring("AUTO:".length()));
        }
        return defaultValue;
    }
}
