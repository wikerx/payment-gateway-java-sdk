package com.scott.payment.sdk.logging;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentGatewayLogSanitizer
 * @date : 2026-06-30 10:28
 * @email : scott_x@163.com
 * @description : OpenAPI SDK 日志辅助工具，负责按商户联调要求输出 Header 副本、卡号脱敏值和 body 摘要。
 *                本类不参与请求签名、加密、资金处理或外部渠道调用；除卡号外，商户号、JWT、账户号和业务字段默认按原值输出，便于沙盒文档核验。
 * @status : modify
 */
public final class PaymentGatewayLogSanitizer {

    private PaymentGatewayLogSanitizer() {
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
     * 返回账户号原值。
     *
     * @param accountNo 账户号
     * @return 原始账户号
     */
    public static String maskAccountNo(String accountNo) {
        return accountNo;
    }

    /**
     * 返回 token 原值。
     *
     * @param token token 文本
     * @return 原始 token
     */
    public static String maskToken(String token) {
        return token;
    }

    /**
     * 返回密钥原值。
     *
     * @param key 密钥文本
     * @return 原始密钥
     */
    public static String maskKey(String key) {
        return key;
    }

    /**
     * 复制 HTTP Header。
     *
     * 该方法仅用于日志输出，不修改真实请求 Header；Authorization 按原值输出，便于商户核验签名和 JWT 内容。
     *
     * @param headers 原始 HTTP Header
     * @return Header 副本
     */
    public static Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        Map<String, String> sanitized = new LinkedHashMap<String, String>();
        if (headers == null) {
            return sanitized;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sanitized.put(key, value);
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
