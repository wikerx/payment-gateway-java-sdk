package com.scott.payment.sdk.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scott.payment.sdk.config.OpenApiConstants;
import com.scott.payment.sdk.exception.OpenApiCryptoException;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.model.common.OpenApiPayloadParts;

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
     * 该方法负责把商户业务 JSON 明文加密为网关接收的 compact payload，不发起 HTTP 请求，不签发 JWT，不修改支付、退款、代付、客户、资金、密钥或配置状态。
     * 每次调用都会生成新的 AES 会话密钥和 IV，因此同一明文多次加密得到的 data 不应相同；调用方不得依赖本方法做业务幂等判断。
     *
     * @param plainText          请求业务 JSON 明文，允许包含支付、退款、代付等业务字段；不得传入空值
     * @param recipientPublicKey 平台请求公钥，用于 RSA-OAEP-SHA256 加密 AES 会话密钥
     * @return protectedHeader.encryptedAesKey.iv.cipherText.tag 格式的 compact 密文报文
     * @throws NullPointerException plainText 或 recipientPublicKey 为空时抛出
     * @throws OpenApiCryptoException AES-GCM、RSA-OAEP 或 Base64URL 处理失败时抛出，不返回明文、私钥或完整密钥材料
     */
    public String encrypt(String plainText, PublicKey recipientPublicKey) {
        return encryptToParts(plainText, recipientPublicKey).toCompactPayload();
    }

    /**
     * 使用平台公钥加密商户请求明文，并返回可供商户文档核验的拆分字段。
     *
     * 该方法只生成 OpenAPI compact 密文结构，不发起 HTTP 请求，不签发 JWT，不修改支付、退款、代付、客户、资金、密钥或配置状态。
     * 返回的 encryptedAesKey、iv、cipherText、tag 适合沙盒联调和文档核验；生产日志不建议长期输出完整值。
     * 每次调用都会生成新的 AES 会话密钥和 IV，不具备业务幂等语义，商户订单幂等仍应使用自身订单号或业务幂等键完成。
     *
     * @param plainText          请求业务 JSON 明文，通常由支付、退款、代付或客户请求 DTO 序列化得到；不得传入空值
     * @param recipientPublicKey 平台请求公钥，用于 RSA-OAEP-SHA256 加密 AES 会话密钥
     * @return compact payload 拆分字段
     * @throws NullPointerException plainText 或 recipientPublicKey 为空时抛出
     * @throws OpenApiCryptoException AES-GCM 或 RSA-OAEP 处理失败时抛出，不返回明文、私钥或完整密钥材料
     */
    public OpenApiPayloadParts encryptToParts(String plainText, PublicKey recipientPublicKey) {
        Objects.requireNonNull(plainText, "plainText can not be null");
        Objects.requireNonNull(recipientPublicKey, "recipientPublicKey can not be null");
        byte[] contentKey = randomBytes(AES_KEY_BYTES);
        byte[] iv = randomBytes(GCM_IV_BYTES);
        String protectedHeader = encodeProtectedHeader();
        byte[] cipherWithTag = aesGcm(Cipher.ENCRYPT_MODE, contentKey, iv, protectedHeader, plainText.getBytes(StandardCharsets.UTF_8));
        byte[] cipherText = Arrays.copyOf(cipherWithTag, cipherWithTag.length - GCM_TAG_BYTES);
        byte[] tag = Arrays.copyOfRange(cipherWithTag, cipherWithTag.length - GCM_TAG_BYTES, cipherWithTag.length);
        byte[] encryptedKey = rsaOaep(Cipher.ENCRYPT_MODE, recipientPublicKey, contentKey);
        return OpenApiPayloadParts.builder()
                .protectedHeader(protectedHeader)
                .header(decodeProtectedHeader(protectedHeader))
                .encryptedAesKey(base64Url(encryptedKey))
                .iv(base64Url(iv))
                .cipherText(base64Url(cipherText))
                .tag(base64Url(tag))
                .build();
    }

    /**
     * 使用商户响应私钥解密平台响应 data。
     *
     * 该方法只解密网关响应中的 data 字段，不解析业务状态机，不确认支付、退款、代付或资金最终状态，不修改密钥或配置。
     * 解密成功只表示报文完整性和密钥匹配通过，调用方仍需根据响应 code、业务 status 和查询接口确认业务结果。
     *
     * @param compactPayload 网关响应 data，格式为 protectedHeader.encryptedAesKey.iv.cipherText.tag；不得为空
     * @param privateKey     商户响应私钥，用于 RSA-OAEP-SHA256 解密 AES 会话密钥；不得为空
     * @return 响应业务 JSON 明文
     * @throws NullPointerException privateKey 为空时抛出
     * @throws OpenApiCryptoException data 为空、格式非法、header 非法、密钥不匹配或 AES-GCM 认证失败时抛出
     */
    public String decrypt(String compactPayload, PrivateKey privateKey) {
        if (compactPayload == null || compactPayload.trim().isEmpty()) {
            throw new OpenApiCryptoException("OpenAPI encrypted data can not be blank");
        }
        Objects.requireNonNull(privateKey, "privateKey can not be null");
        OpenApiPayloadParts parts = splitCompactPayload(compactPayload);
        byte[] contentKey = rsaOaep(Cipher.DECRYPT_MODE, privateKey, base64UrlDecode(parts.getEncryptedAesKey()));
        byte[] iv = base64UrlDecode(parts.getIv());
        byte[] cipherText = base64UrlDecode(parts.getCipherText());
        byte[] tag = base64UrlDecode(parts.getTag());
        byte[] plainText = aesGcm(Cipher.DECRYPT_MODE, contentKey, iv, parts.getProtectedHeader(), concat(cipherText, tag));
        return new String(plainText, StandardCharsets.UTF_8);
    }

    /**
     * 拆分 OpenAPI compact payload。
     *
     * 该方法不解密业务明文，只把 data 按 protectedHeader.encryptedAesKey.iv.cipherText.tag 拆分，
     * 便于商户在沙盒联调、文档示例或问题排查时单独查看 encryptedAesKey、iv、cipherText 和 tag。
     * 该方法不发起 HTTP 请求，不修改支付、退款、代付、客户、资金、密钥或配置状态，不具备业务幂等语义。
     *
     * @param compactPayload 网关请求或响应中的 data 字符串
     * @return compact payload 拆分字段
     * @throws OpenApiCryptoException data 为空、分段数量非法或 protected header 不符合协议时抛出
     */
    public OpenApiPayloadParts splitCompactPayload(String compactPayload) {
        if (compactPayload == null || compactPayload.trim().isEmpty()) {
            throw new OpenApiCryptoException("OpenAPI encrypted data can not be blank");
        }
        String[] parts = compactPayload.split("\\.", -1);
        if (parts.length != COMPACT_PARTS) {
            throw new OpenApiCryptoException("OpenAPI encrypted data format is invalid");
        }
        validateProtectedHeader(parts[0]);
        return OpenApiPayloadParts.builder()
                .protectedHeader(parts[0])
                .header(decodeProtectedHeader(parts[0]))
                .encryptedAesKey(parts[1])
                .iv(parts[2])
                .cipherText(parts[3])
                .tag(parts[4])
                .build();
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
        decodeProtectedHeader(protectedHeader);
    }

    /**
     * 解码并校验 compact payload 的 protected header。
     *
     * @param protectedHeader base64url 编码后的 protected header
     * @return protected header JSON 文本
     * @throws OpenApiCryptoException header 不可解析或协议字段不匹配时抛出
     */
    private String decodeProtectedHeader(String protectedHeader) {
        try {
            String headerJson = new String(base64UrlDecode(protectedHeader), StandardCharsets.UTF_8);
            Map<String, String> header = JsonSupport.fromJson(headerJson, new TypeReference<Map<String, String>>() {
            });
            if (!OpenApiConstants.PAYLOAD_TYPE.equals(header.get(OpenApiConstants.PAYLOAD_HEADER_TYPE))
                    || !KEY_ENCRYPTION_ALGORITHM.equals(header.get(OpenApiConstants.PAYLOAD_HEADER_ALGORITHM))
                    || !CONTENT_ENCRYPTION_ALGORITHM.equals(header.get(OpenApiConstants.PAYLOAD_HEADER_ENCRYPTION))) {
                throw new OpenApiCryptoException("OpenAPI encrypted data header is invalid");
            }
            return headerJson;
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
