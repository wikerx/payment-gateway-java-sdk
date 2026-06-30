package com.wikerx.payment.gateway.sdk.logging;

import org.apache.commons.lang3.StringUtils;

/**
 * OpenAPI SDK 日志脱敏工具，用于避免商户号、JWT、密钥、卡号、CVV 和账户号完整进入日志。
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
