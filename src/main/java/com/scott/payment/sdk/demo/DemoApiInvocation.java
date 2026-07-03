package com.scott.payment.sdk.demo;

/**
 * Demo API invocation result shown below the request form.
 */
public class DemoApiInvocation {

    private final boolean success;
    private final String summary;
    private final String requestJson;
    private final String responseJson;
    private final String errorMessage;

    private DemoApiInvocation(boolean success,
                              String summary,
                              String requestJson,
                              String responseJson,
                              String errorMessage) {
        this.success = success;
        this.summary = summary;
        this.requestJson = requestJson;
        this.responseJson = responseJson;
        this.errorMessage = errorMessage;
    }

    public static DemoApiInvocation success(String summary, String requestJson, String responseJson) {
        return new DemoApiInvocation(true, summary, requestJson, responseJson, null);
    }

    public static DemoApiInvocation failure(String summary, String requestJson, String errorMessage) {
        return new DemoApiInvocation(false, summary, requestJson, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSummary() {
        return summary;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
