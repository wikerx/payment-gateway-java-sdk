package com.scott.payment.sdk;

import com.scott.payment.sdk.exception.PaymentGatewayConfigException;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * 商户 OpenAPI 安全材料，包含商户号、JWT 签名密钥、平台请求公钥和商户响应私钥。
 */
@Getter
@ToString
public final class PaymentGatewayCredentials {

    /**
     * 平台商户号，必须与请求体 merchantInfo.merchantId 和 JWT merchantId claim 保持一致。
     */
    private final String merchantId;

    /**
     * 商户 JWT HS256 签名密钥。该值只用于本地签名，禁止进入日志、异常消息或 toString。
     */
    @ToString.Exclude
    private final String merchantJwtSecret;

    /**
     * 平台请求公钥。SDK 用它加密请求 data，不需要也不应该持有平台请求私钥。
     */
    @ToString.Exclude
    private final String platformPublicKey;

    /**
     * 商户响应私钥。SDK 用它解密平台响应 data，必须由商户服务端安全保存。
     */
    @ToString.Exclude
    private final String merchantResponsePrivateKey;

    /**
     * 创建商户安全材料。
     *
     * @param merchantId                 平台商户号
     * @param merchantJwtSecret          商户 JWT HS256 签名密钥
     * @param platformPublicKey          平台请求公钥，支持 PEM 或 X.509 DER Base64
     * @param merchantResponsePrivateKey 商户响应私钥，支持 PEM 或 PKCS#8 DER Base64
     */
    public PaymentGatewayCredentials(String merchantId,
                              String merchantJwtSecret,
                              String platformPublicKey,
                              String merchantResponsePrivateKey) {
        this.merchantId = requireText(merchantId, "merchantId");
        this.merchantJwtSecret = requireText(merchantJwtSecret, "merchantJwtSecret");
        this.platformPublicKey = requireText(platformPublicKey, "platformPublicKey");
        this.merchantResponsePrivateKey = requireText(merchantResponsePrivateKey, "merchantResponsePrivateKey");
    }

    private static String requireText(String value, String fieldName) {
        if (Objects.isNull(value) || value.trim().isEmpty()) {
            throw new PaymentGatewayConfigException(fieldName + " can not be blank");
        }
        return value.trim();
    }
}
