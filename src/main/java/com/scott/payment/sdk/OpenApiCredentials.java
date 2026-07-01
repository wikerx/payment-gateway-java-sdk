package com.scott.payment.sdk;

import com.scott.payment.sdk.exception.OpenApiConfigException;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiCredentials
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : 商户 OpenAPI 安全材料，负责集中承载商户号、JWT 签名密钥、平台请求公钥和商户响应私钥。
 *                本类只表达 SDK 本地安全配置，不负责发起请求、执行加解密算法、修改资金状态或处理外部渠道回调。
 *                JWT 密钥和商户响应私钥属于敏感数据，必须从 toString、普通日志和异常消息中排除。
 * @status : create
 */
@Getter
@ToString
public final class OpenApiCredentials {

    /**
     * 平台商户号，必须与 JWT merchantId claim 保持一致。
     *
     * 敏感字段：否。
     * 是否允许为空：否。
     * 用途：参与网关商户身份识别和环境数据隔离。
     */
    private final String merchantId;

    /**
     * 商户 JWT HS256 签名密钥。
     *
     * 敏感字段：是。
     * 是否允许为空：否。
     * 用途：生成 `Authorization: Bearer <jwt>`。
     * 限制：禁止进入日志、异常消息或 toString。
     */
    @ToString.Exclude
    private final String merchantJwtSecret;

    /**
     * 平台请求公钥，支持 PEM 或 X.509 DER Base64 文本。
     *
     * 敏感字段：按密钥材料处理。
     * 是否允许为空：否。
     * 用途：SDK 用于加密请求 data。
     * 限制：SDK 不持有平台请求私钥。
     */
    @ToString.Exclude
    private final String platformPublicKey;

    /**
     * 商户响应私钥，支持 PEM 或 PKCS#8 DER Base64 文本。
     *
     * 敏感字段：是。
     * 是否允许为空：否。
     * 用途：SDK 用于解密平台响应 data。
     * 限制：必须由商户服务端安全保存，不得进入日志或客户端环境。
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
    public OpenApiCredentials(String merchantId,
                              String merchantJwtSecret,
                              String platformPublicKey,
                              String merchantResponsePrivateKey) {
        this.merchantId = requireText(merchantId, "merchantId");
        this.merchantJwtSecret = requireText(merchantJwtSecret, "merchantJwtSecret");
        this.platformPublicKey = requireText(platformPublicKey, "platformPublicKey");
        this.merchantResponsePrivateKey = requireText(merchantResponsePrivateKey, "merchantResponsePrivateKey");
    }

    /**
     * 校验密钥配置文本非空。
     *
     * @param value 配置值
     * @param fieldName 字段名
     * @return 去除首尾空白后的配置值
     * @throws OpenApiConfigException 配置值为空时抛出
     */
    private static String requireText(String value, String fieldName) {
        if (Objects.isNull(value) || value.trim().isEmpty()) {
            throw new OpenApiConfigException(fieldName + " can not be blank");
        }
        return value.trim();
    }
}
