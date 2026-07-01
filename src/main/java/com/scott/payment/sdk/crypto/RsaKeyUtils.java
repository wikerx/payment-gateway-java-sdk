package com.scott.payment.sdk.crypto;

import com.scott.payment.sdk.exception.OpenApiCryptoException;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : RsaKeyUtils
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : RSA 密钥解析工具，负责将 PEM 或 DER Base64 文本转换为 X.509 公钥、PKCS#8 私钥或 PEM 输出文本。
 *                本类不生成密钥、不轮换密钥、不访问远程服务；私钥文本属于敏感数据，不得写入普通日志或异常消息。
 * @status : modify
 */
public final class RsaKeyUtils {

    /**
     * RSA 密钥算法名称。
     */
    private static final String RSA = "RSA";
    /**
     * PEM 输出每行字符数。
     */
    private static final int PEM_LINE_LENGTH = 64;
    /**
     * 匹配带 PEM 头尾的 RSA 公钥或私钥块。
     */
    private static final Pattern PEM_BLOCK_PATTERN = Pattern.compile(
            "-----BEGIN (?:PUBLIC|PRIVATE) KEY-----.+?-----END (?:PUBLIC|PRIVATE) KEY-----",
            Pattern.DOTALL);

    private RsaKeyUtils() {
    }

    /**
     * 读取 X.509 RSA 公钥。
     *
     * @param value PEM 或 X.509 DER Base64 公钥
     * @return RSA 公钥
     */
    public static PublicKey readPublicKey(String value) {
        try {
            byte[] encoded = Base64.getDecoder().decode(normalizePem(value));
            return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(encoded));
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new OpenApiCryptoException("OpenAPI platform public key can not be parsed", exception);
        }
    }

    /**
     * 读取 PKCS#8 RSA 私钥。
     *
     * @param value PEM 或 PKCS#8 DER Base64 私钥
     * @return RSA 私钥
     */
    public static PrivateKey readPrivateKey(String value) {
        try {
            byte[] encoded = Base64.getDecoder().decode(normalizePem(value));
            return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new OpenApiCryptoException("OpenAPI merchant response private key can not be parsed", exception);
        }
    }

    /**
     * 将 DER Base64 转为公钥 PEM。
     *
     * @param base64 X.509 DER Base64 公钥
     * @return 公钥 PEM
     */
    public static String toPublicKeyPem(String base64) {
        return toPem(base64, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
    }

    /**
     * 将 DER Base64 转为私钥 PEM。
     *
     * @param base64 PKCS#8 DER Base64 私钥
     * @return 私钥 PEM
     */
    public static String toPrivateKeyPem(String base64) {
        return toPem(base64, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
    }

    /**
     * 标准化 PEM 或 DER Base64 密钥文本。
     *
     * @param value PEM 或 DER Base64 文本
     * @return 去除头尾和空白后的 DER Base64 文本
     */
    private static String normalizePem(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new OpenApiCryptoException("OpenAPI key can not be blank");
        }
        // 平台可能导出带 metadata 的 PEM，这里只保留真正 PEM 块再剥离头尾。
        return extractPemBlock(value)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
    }

    /**
     * 从带 metadata 的 PEM 导出文本中截取密钥块。
     *
     * @param value PEM 或 DER Base64 文本
     * @return PEM 密钥块或原始文本
     */
    private static String extractPemBlock(String value) {
        Matcher matcher = PEM_BLOCK_PATTERN.matcher(value);
        if (matcher.find()) {
            return matcher.group();
        }
        return value;
    }

    /**
     * 将 DER Base64 文本转换为 PEM 格式。
     *
     * @param value DER Base64 文本
     * @param begin PEM 起始行
     * @param end PEM 结束行
     * @return PEM 文本
     */
    private static String toPem(String value, String begin, String end) {
        String normalized = normalizePem(value);
        StringBuilder builder = new StringBuilder(begin).append('\n');
        // PEM 每行 64 个 Base64 字符，便于复制到常见密钥管理工具。
        for (int index = 0; index < normalized.length(); index += PEM_LINE_LENGTH) {
            builder.append(normalized, index, Math.min(index + PEM_LINE_LENGTH, normalized.length())).append('\n');
        }
        return builder.append(end).toString();
    }
}
