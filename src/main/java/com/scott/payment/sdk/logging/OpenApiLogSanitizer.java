package com.scott.payment.sdk.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.scott.payment.sdk.json.JsonSupport;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiLogSanitizer
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 日志辅助工具，负责 Header、业务请求对象、密文外壳和响应对象的日志脱敏与摘要处理。
 *                本类不参与请求签名、加密、资金处理或外部渠道调用；卡号、CVC、JWT、邮箱、手机号、证件号和密钥类字段不得完整进入默认日志。
 * @status : modify
 */
public final class OpenApiLogSanitizer {

    /**
     * 日志敏感字段统一替换值。
     */
    private static final String MASK = "******";

    private OpenApiLogSanitizer() {
    }

    /**
     * 返回商户号原值。
     *
     * @param merchantId 商户号
     * @return 原始商户号
     */
    public static String maskMerchantId(String merchantId) {
        return merchantId;
    }

    /**
     * 脱敏卡号，保留前六位和后四位。
     *
     * @param cardNo 卡号
     * @return 脱敏后的卡号
     */
    public static String maskCardNo(String cardNo) {
        return maskMiddle(cardNo, 6, 4, "******");
    }

    /**
     * 脱敏账户号，保留后四位。
     *
     * @param accountNo 账户号
     * @return 脱敏后的账户号
     */
    public static String maskAccountNo(String accountNo) {
        return maskMiddle(accountNo, 0, 4, MASK);
    }

    /**
     * 脱敏 token，保留前十位和后六位。
     *
     * @param token token 文本
     * @return 脱敏后的 token
     */
    public static String maskToken(String token) {
        return maskMiddle(token, 10, 6, MASK);
    }

    /**
     * 脱敏密钥文本，仅保留长度。
     *
     * @param key 密钥文本
     * @return 脱敏后的密钥摘要
     */
    public static String maskKey(String key) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        return "length=" + key.trim().length();
    }

    /**
     * 复制 HTTP Header 并脱敏 Authorization。
     *
     * 该方法仅用于日志输出，不修改真实请求 Header；Authorization 不输出完整 JWT。
     *
     * @param headers 原始 HTTP Header
     * @return Header 脱敏副本
     */
    public static Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        Map<String, String> sanitized = new LinkedHashMap<String, String>();
        if (headers == null) {
            return sanitized;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("Authorization".equalsIgnoreCase(key)) {
                sanitized.put(key, maskAuthorization(value));
            } else {
                sanitized.put(key, value);
            }
        }
        return sanitized;
    }

    /**
     * 递归脱敏日志对象。
     *
     * 该方法用于业务明文请求、业务明文响应和 HTTP body 对象日志，不会修改原始业务对象。
     *
     * @param value 原始对象
     * @return 脱敏后的 Map、List、基础类型或摘要值
     */
    public static Object sanitizeObject(Object value) {
        if (value == null) {
            return null;
        }
        JsonNode node = JsonSupport.objectMapper().valueToTree(value);
        return sanitizeJsonNode(null, node);
    }

    /**
     * 生成加密 data 字段摘要。
     *
     * @param data compact 密文 data
     * @return data 摘要
     */
    public static String encryptedDataSummary(String data) {
        if (StringUtils.isBlank(data)) {
            return "";
        }
        return "length=" + data.length() + ", parts=" + data.split("\\.", -1).length;
    }

    /**
     * 返回 JSON body 日志摘要。
     *
     * 摘要只暴露长度和是否包含 OpenAPI 加密 data 字段，避免完整密文、明文请求或响应 data 进入普通日志。
     *
     * @param body 请求或响应 body
     * @return body 摘要
     */
    public static String bodySummary(String body) {
        if (body == null) {
            return "null";
        }
        return "length=" + body.length() + ", encryptedData=" + body.contains("\"data\"");
    }

    private static String maskAuthorization(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        int index = trimmed.indexOf(' ');
        if (index > 0 && index + 1 < trimmed.length()) {
            return trimmed.substring(0, index + 1) + maskToken(trimmed.substring(index + 1));
        }
        return maskToken(trimmed);
    }

    private static Object sanitizeJsonNode(String fieldName, JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                Object sanitizedValue = sanitizeJsonNode(entry.getKey(), entry.getValue());
                if (sanitizedValue != null) {
                    result.put(entry.getKey(), sanitizedValue);
                }
            }
            return result;
        }
        if (node.isArray()) {
            List<Object> result = new ArrayList<Object>();
            for (JsonNode item : node) {
                result.add(sanitizeJsonNode(fieldName, item));
            }
            return result;
        }
        if (node.isTextual()) {
            return sanitizeText(fieldName, node.asText());
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }

    private static Object sanitizeText(String fieldName, String value) {
        if (isBlankField(fieldName)) {
            return value;
        }
        String normalized = fieldName.toLowerCase(java.util.Locale.ROOT);
        if (isKeyField(normalized)) {
            return maskKey(value);
        }
        if (isCardNumberField(normalized)) {
            return maskCardNo(value);
        }
        if (isCvcField(normalized)) {
            return MASK;
        }
        if (isEmailField(normalized)) {
            return maskEmail(value);
        }
        if (isPhoneField(normalized) || isIdentityField(normalized)) {
            return maskMiddle(value, 2, 2, MASK);
        }
        if ("authorization".equals(normalized)) {
            return maskAuthorization(value);
        }
        return value;
    }

    private static boolean isBlankField(String fieldName) {
        return StringUtils.isBlank(fieldName);
    }

    private static boolean isKeyField(String normalized) {
        return normalized.contains("key")
                || normalized.contains("secret")
                || normalized.contains("private")
                || normalized.contains("token");
    }

    private static boolean isCardNumberField(String normalized) {
        return "number".equals(normalized)
                || normalized.contains("cardno")
                || normalized.contains("cardnumber")
                || normalized.contains("accountno");
    }

    private static boolean isCvcField(String normalized) {
        return "cvc".equals(normalized) || "cvv".equals(normalized);
    }

    private static boolean isEmailField(String normalized) {
        return normalized.contains("email");
    }

    private static boolean isPhoneField(String normalized) {
        return normalized.contains("phone") || normalized.contains("mobile");
    }

    private static boolean isIdentityField(String normalized) {
        return normalized.contains("identity") || normalized.contains("idcard") || normalized.contains("document");
    }

    private static String maskEmail(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String text = value.trim();
        int atIndex = text.indexOf('@');
        if (atIndex <= 1) {
            return MASK;
        }
        return text.charAt(0) + MASK + text.substring(atIndex);
    }

    private static String maskMiddle(String value, int prefixLength, int suffixLength, String mask) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= prefixLength + suffixLength) {
            // 短敏感值不保留任何明文片段，避免小商户号或短 token 被还原。
            return repeat('*', text.length());
        }
        return text.substring(0, prefixLength)
                + mask
                + text.substring(text.length() - suffixLength);
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
