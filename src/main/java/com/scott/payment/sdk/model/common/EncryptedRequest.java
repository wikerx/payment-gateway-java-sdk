package com.scott.payment.sdk.model.common;

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
     * 是否生产模式，false 为沙盒，true 为生产。
     */
    private Boolean livemode;

    /**
     * compact 密文报文，格式为 protectedHeader.encryptedKey.iv.cipherText.tag。
     */
    @ToString.Exclude
    private String data;
}
