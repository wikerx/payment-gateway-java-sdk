package com.scott.payment.sdk.demo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backing form for one demo API page.
 */
public class DemoApiForm {

    private Map<String, String> params = new LinkedHashMap<String, String>();

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params == null ? new LinkedHashMap<String, String>() : params;
    }
}
