package com.scott.payment.sdk.demo;

import java.util.Collections;
import java.util.List;

/**
 * Demo API group shown on the index page.
 */
public class DemoApiGroup {

    private final String code;
    private final String name;
    private final String description;
    private final List<DemoApiDefinition> apis;

    public DemoApiGroup(String code, String name, String description, List<DemoApiDefinition> apis) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.apis = Collections.unmodifiableList(apis);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<DemoApiDefinition> getApis() {
        return apis;
    }
}
