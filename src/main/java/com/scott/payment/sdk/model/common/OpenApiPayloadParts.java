package com.scott.payment.sdk.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiPayloadParts
 * @date : 2026-07-01 14:12
 * @email : scott_x@163.com
 * @description : OpenAPI compact 加密 payload 拆分结果，负责表达 data 字段中的 protectedHeader、header、encryptedAesKey、iv、cipherText 和 tag。
 *                本类只承载密文结构拆分结果，不执行加解密算法，不发起 HTTP 请求，不读取或轮换密钥，不修改支付、退款、代付、客户或资金状态。
 *                encryptedAesKey 和 cipherText 属于密文调试材料，适用于商户沙盒联调、文档核验和受控排查，不参与商户侧签名、对账、清分或结算。
 * @status : create
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenApiPayloadParts {

    /**
     * compact payload 第一段，Base64URL 无 padding 编码后的 protected header。
     *
     * 格式：Base64URL(JSON)。
     * 敏感字段：否。
     * 是否允许为空：否。
     * 是否来自外部渠道：否，来自 SDK 或网关 OpenAPI 加密协议。
     * 用途：作为 AES-GCM AAD 参与完整性认证。
     * 是否参与签名、加密、对账、清分或结算：参与 AES-GCM 认证，不参与 JWT 签名、资金对账、清分或结算。
     */
    private String protectedHeader;

    /**
     * protected header 解码后的 JSON 文本。
     *
     * 格式：JSON 字符串，包含 typ、alg、enc。
     * 敏感字段：否。
     * 是否允许为空：否。
     * 是否来自外部渠道：否，由 protectedHeader 解码得到。
     * 用途：商户联调时核验 typ、alg、enc 协议字段。
     * 是否参与签名、加密、对账、清分或结算：不直接参与签名或资金处理，原始 protectedHeader 参与 AES-GCM 认证。
     */
    private String header;

    /**
     * RSA-OAEP-SHA256 加密后的 AES 会话密钥。
     *
     * 格式：Base64URL 无 padding 字符串。
     * 敏感字段：是。
     * 是否允许为空：否。
     * 是否来自外部渠道：请求侧由商户 SDK 生成，响应侧由网关生成。
     * 用途：平台或商户侧使用对应 RSA 私钥解出 AES 会话密钥。
     * 是否参与签名、加密、对账、清分或结算：参与 AES 会话密钥交换，不参与 JWT 签名、资金对账、清分或结算。
     * 限制：仅用于沙盒联调、文档核验和受控排查，不建议生产日志长期输出完整值。
     */
    @ToString.Exclude
    private String encryptedAesKey;

    /**
     * AES-GCM 初始化向量，Base64URL 无 padding 编码。
     *
     * 格式：12 字节随机 IV 的 Base64URL 文本。
     * 敏感字段：否。
     * 是否允许为空：否。
     * 是否来自外部渠道：请求侧由商户 SDK 生成，响应侧由网关生成。
     * 用途：参与 AES-GCM 加解密。
     * 是否参与签名、加密、对账、清分或结算：参与 AES-GCM 加解密，不参与 JWT 签名、资金对账、清分或结算。
     */
    private String iv;

    /**
     * AES-GCM 业务密文字段，Base64URL 无 padding 编码。
     *
     * 格式：Base64URL 无 padding 字符串。
     * 敏感字段：是。
     * 是否允许为空：否。
     * 是否来自外部渠道：请求侧由商户 SDK 生成，响应侧由网关生成。
     * 用途：承载业务明文 JSON 加密后的密文。
     * 是否参与签名、加密、对账、清分或结算：参与 OpenAPI 报文加密，不参与 JWT 签名、资金对账、清分或结算。
     * 限制：仅用于沙盒联调、文档核验和受控排查，不建议生产日志长期输出完整值。
     */
    @ToString.Exclude
    private String cipherText;

    /**
     * AES-GCM 认证标签，Base64URL 无 padding 编码。
     *
     * 格式：16 字节 GCM tag 的 Base64URL 文本。
     * 敏感字段：否。
     * 是否允许为空：否。
     * 是否来自外部渠道：请求侧由商户 SDK 生成，响应侧由网关生成。
     * 用途：校验密文和 protected header 未被篡改。
     * 是否参与签名、加密、对账、清分或结算：参与 AES-GCM 完整性校验，不参与 JWT 签名、资金对账、清分或结算。
     */
    private String tag;

    /**
     * 将拆分字段重新组装为网关协议使用的 compact payload。
     *
     * 该方法只拼接已有字段，不生成新密钥、不执行加密或解密、不发起 HTTP 请求、不修改支付、退款、代付、客户、资金、密钥或配置状态。
     * 调用方必须保证各字段来自同一次 compact payload 拆分或加密结果，避免混用字段导致网关解密失败。
     *
     * @return protectedHeader.encryptedAesKey.iv.cipherText.tag 格式的 data 字符串
     */
    public String toCompactPayload() {
        return String.join(".", protectedHeader, encryptedAesKey, iv, cipherText, tag);
    }
}
