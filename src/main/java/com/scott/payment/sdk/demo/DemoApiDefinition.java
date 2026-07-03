package com.scott.payment.sdk.demo;

import com.scott.payment.sdk.api.OpenApiEndpoint;

import java.util.Collections;
import java.util.List;

/**
 * Demo page metadata for one merchant OpenAPI endpoint.
 */
public class DemoApiDefinition {

    private final String code;
    private final String groupCode;
    private final String groupName;
    private final String name;
    private final String description;
    private final String actionLabel;
    private final OpenApiEndpoint endpoint;
    private final List<DemoApiField> requestFields;
    private final List<DemoApiField> responseFields;

    public DemoApiDefinition(String code,
                             String groupCode,
                             String groupName,
                             String name,
                             String description,
                             String actionLabel,
                             OpenApiEndpoint endpoint,
                             List<DemoApiField> requestFields,
                             List<DemoApiField> responseFields) {
        this.code = code;
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.name = name;
        this.description = description;
        this.actionLabel = actionLabel;
        this.endpoint = endpoint;
        this.requestFields = Collections.unmodifiableList(requestFields);
        this.responseFields = Collections.unmodifiableList(responseFields);
    }

    public String getCode() {
        return code;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public OpenApiEndpoint getEndpoint() {
        return endpoint;
    }

    public List<DemoApiField> getRequestFields() {
        return requestFields;
    }

    public List<DemoApiField> getResponseFields() {
        return responseFields;
    }
}
