package com.wikerx.payment.gateway.sdk.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * OpenAPI 密文请求体，业务明文加密后放入 data 字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncryptedRequest {

    /**
     * compact 密文报文，格式为 protectedHeader.encryptedKey.iv.cipherText.tag。
     */
    @ToString.Exclude
    private String data;
}
