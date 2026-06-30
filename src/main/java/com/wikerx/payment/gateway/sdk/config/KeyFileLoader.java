package com.wikerx.payment.gateway.sdk.config;

import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayConfigException;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAPI RSA 密钥文本加载器，统一支持 classpath、file URI、普通文件路径和直接 Base64 文本。
 */
public final class KeyFileLoader {

    /**
     * classpath 路径前缀。
     */
    private static final String CLASSPATH_PREFIX = "classpath:";
    /**
     * file 路径前缀。
     */
    private static final String FILE_PREFIX = "file:";
    /**
     * 平台导出的 PEM 文件可能在 BEGIN 行前附带 merNo、keyVersion 等 metadata。
     */
    private static final Pattern PEM_BLOCK_PATTERN = Pattern.compile(
            "-----BEGIN (?:PUBLIC|PRIVATE) KEY-----.+?-----END (?:PUBLIC|PRIVATE) KEY-----",
            Pattern.DOTALL);

    private KeyFileLoader() {
    }

    /**
     * 按整改约定解析密钥配置：优先读取 `xxx-file` 指向的 PEM 文件，未配置时回退到 `xxx` 文本值。
     *
     * @param keyLocation classpath、file URI 或普通文件路径
     * @param inlineValue 配置文件中的 Base64 或 PEM 文本
     * @param fieldName   用于错误提示的配置项名称
     * @return 去掉 PEM 头尾和空白后的密钥 Base64 文本
     */
    public static String resolve(String keyLocation, String inlineValue, String fieldName) {
        if (StringUtils.isNotBlank(keyLocation)) {
            return load(keyLocation, fieldName);
        }
        if (StringUtils.isNotBlank(inlineValue)) {
            return normalizePem(inlineValue);
        }
        throw new PaymentGatewayConfigException(fieldName + " or " + fieldName + "-file can not be blank");
    }

    /**
     * 加载一个密钥位置。`classpath:` 从运行时 classpath 读取，`file:` 和普通路径从文件系统读取。
     *
     * @param keyLocation classpath、file URI 或普通文件路径
     * @param fieldName   用于错误提示的配置项名称
     * @return 去掉 PEM 头尾和空白后的密钥 Base64 文本
     */
    public static String load(String keyLocation, String fieldName) {
        String location = requireText(keyLocation, fieldName + "-file");
        try {
            if (location.startsWith(CLASSPATH_PREFIX)) {
                return normalizePem(readClasspath(location.substring(CLASSPATH_PREFIX.length()), fieldName));
            }
            if (location.startsWith(FILE_PREFIX)) {
                return normalizePem(new String(Files.readAllBytes(Paths.get(URI.create(location))), StandardCharsets.UTF_8));
            }
            return normalizePem(new String(Files.readAllBytes(Paths.get(location)), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException | IOException exception) {
            throw new PaymentGatewayConfigException(fieldName + "-file can not be loaded", exception);
        }
    }

    /**
     * 去掉 PEM 头尾和换行，仅保留可交给 RSA 解析器处理的 DER Base64 文本。
     *
     * @param value PEM 或 DER Base64 文本
     * @return DER Base64 文本
     */
    public static String normalizePem(String value) {
        String text = requireText(value, "key");
        String normalized = extractPemBlock(text)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        if (StringUtils.isBlank(normalized)) {
            throw new PaymentGatewayConfigException("OpenAPI key content can not be blank");
        }
        return normalized;
    }

    /**
     * 从平台导出的带 metadata PEM 内容中提取真正密钥块。
     *
     * @param text 原始密钥文本
     * @return PEM 密钥块或原文本
     */
    private static String extractPemBlock(String text) {
        Matcher matcher = PEM_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return text;
    }

    private static String readClasspath(String path, String fieldName) throws IOException {
        String resourcePath = requireText(path, fieldName + "-file");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new PaymentGatewayConfigException(fieldName + "-file can not be found in classpath");
            }
            return new String(readAll(inputStream), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    private static String requireText(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            throw new PaymentGatewayConfigException(fieldName + " can not be blank");
        }
        return value.trim();
    }
}
