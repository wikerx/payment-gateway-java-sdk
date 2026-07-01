package com.scott.payment.sdk.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scott.payment.sdk.config.PaymentGatewayConstants;
import com.scott.payment.sdk.exception.PaymentGatewayCryptoException;
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
 * OpenAPI 报文混合加解密工具，对齐后端 RSA-OAEP-SHA256 + AES-256-GCM compact 协议。
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
            throw new PaymentGatewayCryptoException("OpenAPI encrypted data can not be blank");
        }
        Objects.requireNonNull(privateKey, "privateKey can not be null");
        String[] parts = compactPayload.split("\\.", -1);
        if (parts.length != COMPACT_PARTS) {
            throw new PaymentGatewayCryptoException("OpenAPI encrypted data format is invalid");
        }
        validateProtectedHeader(parts[0]);
        byte[] contentKey = rsaOaep(Cipher.DECRYPT_MODE, privateKey, base64UrlDecode(parts[1]));
        byte[] iv = base64UrlDecode(parts[2]);
        byte[] cipherText = base64UrlDecode(parts[3]);
        byte[] tag = base64UrlDecode(parts[4]);
        byte[] plainText = aesGcm(Cipher.DECRYPT_MODE, contentKey, iv, parts[0], concat(cipherText, tag));
        return new String(plainText, StandardCharsets.UTF_8);
    }

    private String encodeProtectedHeader() {
        // compact 第一段参与 AES-GCM AAD 校验，服务端会校验 typ/alg/enc 三个固定协议字段。
        Map<String, String> header = new LinkedHashMap<>();
        header.put(PaymentGatewayConstants.PAYLOAD_HEADER_TYPE, PaymentGatewayConstants.PAYLOAD_TYPE);
        header.put(PaymentGatewayConstants.PAYLOAD_HEADER_ALGORITHM, KEY_ENCRYPTION_ALGORITHM);
        header.put(PaymentGatewayConstants.PAYLOAD_HEADER_ENCRYPTION, CONTENT_ENCRYPTION_ALGORITHM);
        return base64Url(JsonSupport.toJson(header).getBytes(StandardCharsets.UTF_8));
    }

    private void validateProtectedHeader(String protectedHeader) {
        try {
            String headerJson = new String(base64UrlDecode(protectedHeader), StandardCharsets.UTF_8);
            Map<String, String> header = JsonSupport.fromJson(headerJson, new TypeReference<Map<String, String>>() {
            });
            if (!PaymentGatewayConstants.PAYLOAD_TYPE.equals(header.get(PaymentGatewayConstants.PAYLOAD_HEADER_TYPE))
                    || !KEY_ENCRYPTION_ALGORITHM.equals(header.get(PaymentGatewayConstants.PAYLOAD_HEADER_ALGORITHM))
                    || !CONTENT_ENCRYPTION_ALGORITHM.equals(header.get(PaymentGatewayConstants.PAYLOAD_HEADER_ENCRYPTION))) {
                throw new PaymentGatewayCryptoException("OpenAPI encrypted data header is invalid");
            }
        } catch (RuntimeException exception) {
            if (exception instanceof PaymentGatewayCryptoException) {
                throw exception;
            }
            throw new PaymentGatewayCryptoException("OpenAPI encrypted data header can not be parsed", exception);
        }
    }

    private byte[] aesGcm(int mode, byte[] contentKey, byte[] iv, String protectedHeader, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(mode, new SecretKeySpec(contentKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            // 使用 protected header 作为 AAD，防止密文头被篡改后仍能通过解密。
            cipher.updateAAD(protectedHeader.getBytes(StandardCharsets.US_ASCII));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException exception) {
            throw new PaymentGatewayCryptoException("OpenAPI AES-GCM crypto failed", exception);
        }
    }

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
            throw new PaymentGatewayCryptoException("OpenAPI RSA-OAEP crypto failed", exception);
        }
    }

    private byte[] randomBytes(int length) {
        byte[] value = new byte[length];
        secureRandom.nextBytes(value);
        return value;
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64UrlDecode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new PaymentGatewayCryptoException("OpenAPI base64url data can not be decoded", exception);
        }
    }

    private byte[] concat(byte[] left, byte[] right) {
        byte[] result = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }
}
