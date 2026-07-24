package com.scott.payment.sdk.api.webhook.v2;

import lombok.Data;

/**
 * Webhook v2 encrypted body wrapper.
 */
@Data
public class EncryptedWebhookRequest {

    /**
     * RSA-OAEP-256 + AES-256-GCM compact encrypted callback payload.
     */
    private String data;
}

