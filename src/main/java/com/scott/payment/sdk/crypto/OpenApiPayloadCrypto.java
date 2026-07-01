package com.scott.payment.sdk.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.exception.OpenApiCryptoException;
import com.scott.payment.sdk.json.JsonSupport;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiPayloadCrypto
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI 报文混合加解密组件，负责按 RSA-OAEP-SHA256 + AES-256-GCM compact 协议加密请求 data 和解密响应 data。
 *                本类不签发 JWT、不发起 HTTP 请求、不修改支付、退款、代付或资金状态；明文只应存在于调用链内存中，普通日志不得输出。
 *                加解密失败会抛出 SDK 加解密异常，异常消息不得携带明文、私钥或完整密文。
 * @status : modify
 */
public class OpenApiPayloadCrypto {

    /**
     * RSA 密钥加密算法。
     */
    private static final String KEY_ENCRYPTION_ALGORITHM = "RSA-OAEP-256";
    /**
     * 内容加密算法。
     */
    private static final String CONTENT_ENCRYPTION_ALGORITHM = "A256GCM";
    /**
     * AES-GCM JCE transformation。
     */
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    /**
     * RSA-OAEP JCE transformation。
     */
    private static final String RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPPadding";
    /**
     * AES-256 密钥字节数。
     */
    private static final int AES_KEY_BYTES = 32;
    /**
     * GCM IV 字节数。
     */
    private static final int GCM_IV_BYTES = 12;
    /**
     * GCM 认证标签 bit 数。
     */
    private static final int GCM_TAG_BITS = 128;
    /**
     * GCM 认证标签字节数。
     */
    private static final int GCM_TAG_BYTES = GCM_TAG_BITS / Byte.SIZE;
    /**
     * compact payload 固定分段数。
     */
    private static final int COMPACT_PARTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 使用平台公钥加密商户请求明文。
     *
     * @param plainText          请求业务 JSON 明文
     * @param recipientPublicKey 平台请求公钥
     * @return compact 密文报文
     */
    public String encrypt(String plainText, PublicKey recipientPublicKey) {
        Objects.requireNonNull(plainText, "plainText can not be null");
        Objects.requireNonNull(recipientPublicKey, "recipientPublicKey can not be null");
        byte[] contentKey = randomBytes(AES_KEY_BYTES);
        byte[] iv = randomBytes(GCM_IV_BYTES);
        String protectedHeader = encodeProtectedHeader();
        byte[] cipherWithTag = aesGcm(Cipher.ENCRYPT_MODE, contentKey, iv, protectedHeader, plainText.getBytes(StandardCharsets.UTF_8));
        byte[] cipherText = Arrays.copyOf(cipherWithTag, cipherWithTag.length - GCM_TAG_BYTES);
        byte[] tag = Arrays.copyOfRange(cipherWithTag, cipherWithTag.length - GCM_TAG_BYTES, cipherWithTag.length);
        byte[] encryptedKey = rsaOaep(Cipher.ENCRYPT_MODE, recipientPublicKey, contentKey);
        return String.join(".",
                protectedHeader,
                base64Url(encryptedKey),
                base64Url(iv),
                base64Url(cipherText),
                base64Url(tag));
    }

    /**
     * 使用商户响应私钥解密平台响应 data。
     *
     * @param compactPayload compact 密文报文
     * @param privateKey     商户响应私钥
     * @return 响应业务 JSON 明文
     */
    public String decrypt(String compactPayload, PrivateKey privateKey) {
        if (compactPayload == null || compactPayload.trim().isEmpty()) {
            throw new OpenApiCryptoException("OpenAPI encrypted data can not be blank");
        }
        Objects.requireNonNull(privateKey, "privateKey can not be null");
        String[] parts = compactPayload.split("\\.", -1);
        if (parts.length != COMPACT_PARTS) {
            throw new OpenApiCryptoException("OpenAPI encrypted data format is invalid");
        }
        validateProtectedHeader(parts[0]);
        byte[] contentKey = rsaOaep(Cipher.DECRYPT_MODE, privateKey, base64UrlDecode(parts[1]));
        byte[] iv = base64UrlDecode(parts[2]);
        byte[] cipherText = base64UrlDecode(parts[3]);
        byte[] tag = base64UrlDecode(parts[4]);
        byte[] plainText = aesGcm(Cipher.DECRYPT_MODE, contentKey, iv, parts[0], concat(cipherText, tag));
        return new String(plainText, StandardCharsets.UTF_8);
    }

    /**
     * 生成 compact payload 的 protected header。
     *
     * 该 header 会作为 AES-GCM AAD 参与认证，防止 typ、alg、enc 被篡改。
     *
     * @return base64url 编码后的 protected header
     */
    private String encodeProtectedHeader() {
        // compact 第一段参与 AES-GCM AAD 校验，服务端会校验 typ/alg/enc 三个固定协议字段。
        Map<String, String> header = new LinkedHashMap<>();
        header.put(OpenApiConstants.PAYLOAD_HEADER_TYPE, OpenApiConstants.PAYLOAD_TYPE);
        header.put(OpenApiConstants.PAYLOAD_HEADER_ALGORITHM, KEY_ENCRYPTION_ALGORITHM);
        header.put(OpenApiConstants.PAYLOAD_HEADER_ENCRYPTION, CONTENT_ENCRYPTION_ALGORITHM);
        return base64Url(JsonSupport.toJson(header).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验 compact payload 的 protected header。
     *
     * @param protectedHeader base64url 编码后的 protected header
     * @throws OpenApiCryptoException header 不可解析或协议字段不匹配时抛出
     */
    private void validateProtectedHeader(String protectedHeader) {
        try {
            String headerJson = new String(base64UrlDecode(protectedHeader), StandardCharsets.UTF_8);
            Map<String, String> header = JsonSupport.fromJson(headerJson, new TypeReference<Map<String, String>>() {
            });
            if (!OpenApiConstants.PAYLOAD_TYPE.equals(header.get(OpenApiConstants.PAYLOAD_HEADER_TYPE))
                    || !KEY_ENCRYPTION_ALGORITHM.equals(header.get(OpenApiConstants.PAYLOAD_HEADER_ALGORITHM))
                    || !CONTENT_ENCRYPTION_ALGORITHM.equals(header.get(OpenApiConstants.PAYLOAD_HEADER_ENCRYPTION))) {
                throw new OpenApiCryptoException("OpenAPI encrypted data header is invalid");
            }
        } catch (RuntimeException exception) {
            if (exception instanceof OpenApiCryptoException) {
                throw exception;
            }
            throw new OpenApiCryptoException("OpenAPI encrypted data header can not be parsed", exception);
        }
    }

    /**
     * 执行 AES-256-GCM 加密或解密。
     *
     * @param mode JCE 加解密模式
     * @param contentKey AES 内容密钥
     * @param iv GCM IV
     * @param protectedHeader compact 第一段 header，用作 AAD
     * @param input 明文或密文输入
     * @return 加密或解密后的字节
     */
    private byte[] aesGcm(int mode, byte[] contentKey, byte[] iv, String protectedHeader, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(mode, new SecretKeySpec(contentKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            // 使用 protected header 作为 AAD，防止密文头被篡改后仍能通过解密。
            cipher.updateAAD(protectedHeader.getBytes(StandardCharsets.US_ASCII));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException exception) {
            throw new OpenApiCryptoException("OpenAPI AES-GCM crypto failed", exception);
        }
    }

    /**
     * 执行 RSA-OAEP-SHA256 加密或解密 AES 会话密钥。
     *
     * @param mode JCE 加解密模式
     * @param key RSA 公钥或私钥
     * @param input AES 会话密钥明文或密文
     * @return 处理后的密钥字节
     */
    private byte[] rsaOaep(int mode, Key key, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_TRANSFORMATION);
            // 后端协议要求 OAEP 主摘要和 MGF1 摘要都使用 SHA-256。
            OAEPParameterSpec spec = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT);
            cipher.init(mode, key, spec);
            return cipher.doFinal(input);
        } catch (GeneralSecurityException exception) {
            throw new OpenApiCryptoException("OpenAPI RSA-OAEP crypto failed", exception);
        }
    }

    /**
     * 生成加密所需随机字节。
     *
     * @param length 字节长度
     * @return 安全随机字节
     */
    private byte[] randomBytes(int length) {
        byte[] value = new byte[length];
        secureRandom.nextBytes(value);
        return value;
    }

    /**
     * 执行无 padding 的 base64url 编码。
     *
     * @param value 原始字节
     * @return base64url 文本
     */
    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    /**
     * 解码 base64url 文本。
     *
     * @param value base64url 文本
     * @return 解码后的字节
     */
    private byte[] base64UrlDecode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new OpenApiCryptoException("OpenAPI base64url data can not be decoded", exception);
        }
    }

    /**
     * 拼接 AES-GCM 密文和认证标签。
     *
     * @param left 密文字节
     * @param right 认证标签字节
     * @return 拼接后的字节数组
     */
    private byte[] concat(byte[] left, byte[] right) {
        byte[] result = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }
}
