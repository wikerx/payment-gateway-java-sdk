package com.scott.payment.sdk.demo;

/**
 * Local callback URLs used by the SDK demo console.
 */
public final class DemoLocalUrls {

    /**
     * Demo server address that the gateway can redirect to during local merchant testing.
     */
    public static final String LOCAL_BASE_URL = "http://192.168.2.114:58080/payment-sdk";

    /**
     * Frontend return URL used by payin checkout and local payment demos.
     */
    public static final String PAYIN_RETURN_URL = LOCAL_BASE_URL + "/demo/return";

    /**
     * Payin asynchronous notification URL.
     */
    public static final String PAYIN_NOTIFY_URL = LOCAL_BASE_URL + "/api/webhook/payin";

    /**
     * Payout asynchronous notification URL.
     */
    public static final String PAYOUT_NOTIFY_URL = LOCAL_BASE_URL + "/api/webhook/payout";

    private DemoLocalUrls() {
    }
}
