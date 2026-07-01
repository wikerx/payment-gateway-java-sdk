package com.scott.payment.sdk.logging;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentGatewayLogSanitizer
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 日志脱敏工具，负责对商户号、JWT、API 密钥、卡号、CVV、账户号、请求体和响应体日志做摘要化处理。
 *                本类不参与请求签名、加密、资金处理或外部渠道调用，只服务于日志安全；敏感数据只允许输出脱敏片段、长度或是否包含加密 data 的摘要。
 * @status : modify
 */
public final class PaymentGatewayLogSanitizer {

    /**
     * 账号脱敏保留后缀长度。
     */
    private static final int ACCOUNT_SUFFIX_LENGTH = 4;

    private PaymentGatewayLogSanitizer() {
    }

    /**
     * 脱敏商户号，保留前三位和后三位。
     *
     * @param merchantId 商户号
     * @return 脱敏后的商户号
     */
    public static String maskMerchantId(String merchantId) {
        return maskMiddle(merchantId, 3, 3, "***");
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
     * 脱敏账户号，仅保留后四位。
     *
     * @param accountNo 账户号
     * @return 脱敏后的账户号
     */
    public static String maskAccountNo(String accountNo) {
        if (StringUtils.isBlank(accountNo)) {
            return "";
        }
        String text = accountNo.trim();
        if (text.length() <= ACCOUNT_SUFFIX_LENGTH) {
            return "****";
        }
        return repeat('*', text.length() - ACCOUNT_SUFFIX_LENGTH)
                + text.substring(text.length() - ACCOUNT_SUFFIX_LENGTH);
    }

    /**
     * 脱敏 token，仅保留前六位和后四位。
     *
     * @param token token 文本
     * @return 脱敏后的 token
     */
    public static String maskToken(String token) {
        return maskMiddle(token, 6, 4, "******");
    }

    /**
     * 脱敏密钥，仅保留前六位和后四位。
     *
     * @param key 密钥文本
     * @return 脱敏后的密钥
     */
    public static String maskKey(String key) {
        return maskMiddle(key, 6, 4, "******");
    }

    /**
     * 脱敏 HTTP Header。
     *
     * 该方法仅用于日志输出，不修改真实请求 Header；Authorization 中的 Bearer JWT 或历史 Payment token 只保留前缀和脱敏摘要。
     *
     * @param headers 原始 HTTP Header
     * @return 可安全输出到日志的 Header 副本
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

    private static String maskAuthorization(String authorization) {
        if (StringUtils.isBlank(authorization)) {
            return "";
        }
        String text = authorization.trim();
        int splitIndex = text.indexOf(' ');
        if (splitIndex < 0) {
            return maskToken(text);
        }
        String prefix = text.substring(0, splitIndex + 1);
        String token = text.substring(splitIndex + 1);
        return prefix + maskToken(token);
    }

    private static String maskMiddle(String value, int prefixLength, int suffixLength, String mask) {
        if (StringUtils.isBlank(value)) {
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
