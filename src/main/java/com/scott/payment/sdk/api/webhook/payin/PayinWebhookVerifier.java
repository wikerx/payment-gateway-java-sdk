package com.scott.payment.sdk.api.webhook.payin;

import com.scott.payment.sdk.model.webhook.PayinWebhookRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayinWebhookVerifier
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : 代收异步通知签名校验器，负责按网关回调规则拼接 t、tradeNo、orderNo、currency、amount、status、code、message 并计算 SHA-256 hex。
 *                本类只做本地签名摘要计算和常量时间比较，不做 HTTP 接收、不落库、不修改资金、不推进状态，也不访问外部渠道。
 *                当前代收回调签名规则未使用 merchant-config.properties 中的 API 私钥；如网关文档升级为带密钥签名，需要同步调整本类和测试。
 * @status : create
 */
@Component
public class PayinWebhookVerifier {

    /**
     * 校验代收异步通知签名。
     *
     * 该方法不修改任何业务状态；签名不匹配时返回 false，由 Controller 决定 HTTP 响应。
     * 如果是在 Spring Controller 中接收网关 GET 回调，优先使用 {@link #verify(String, String, Map)}，
     * 避免 amount 等字段在绑定为 BigDecimal 后丢失原始小数位导致验签不一致。
     *
     * @param timestamp Header `t`，网关生成签名时使用的毫秒时间戳
     * @param signature Header `signature`，网关传入的 SHA-256 hex 签名
     * @param request 代收回调参数
     * @return true 表示签名一致，false 表示缺少必要参数或签名不一致
     */
    public boolean verify(String timestamp, String signature, PayinWebhookRequest request) {
        if (StringUtils.isBlank(timestamp) || StringUtils.isBlank(signature) || request == null) {
            return false;
        }
        String expected = sign(timestamp, request);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 使用 HTTP 原始 query/form 参数校验代收异步通知签名。
     *
     * 网关签名时使用的是发送前的字段字符串，例如 amount 可能是 "100"、"100.00" 或 "3.10"。
     * Controller 接收回调时应优先把 request.getParameterMap() 转成首值 Map 后调用本方法，
     * 不要先把 amount 绑定成 BigDecimal 再验签，否则可能因为小数位规范化造成签名不一致。
     *
     * @param timestamp Header `t`，网关生成签名时使用的毫秒时间戳
     * @param signature Header `signature`，网关传入的 SHA-256 hex 签名
     * @param params 原始 query/form 参数首值 Map
     * @return true 表示签名一致，false 表示缺少必要参数或签名不一致
     */
    public boolean verify(String timestamp, String signature, Map<String, String> params) {
        if (StringUtils.isBlank(timestamp) || StringUtils.isBlank(signature) || params == null) {
            return false;
        }
        String expected = sign(timestamp, params);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 计算代收异步通知签名。
     *
     * 签名原文来自网关当前实现：t + tradeNo + orderNo + currency + amount + status + code + message。
     * amount 使用 BigDecimal.stripTrailingZeros().toPlainString()，避免科学计数法影响商户本地验签。
     *
     * @param timestamp Header `t`
     * @param request 代收回调参数
     * @return SHA-256 hex 小写签名
     */
    public String sign(String timestamp, PayinWebhookRequest request) {
        String signSource = buildSignSource(timestamp, request);
        return sha256Hex(signSource);
    }

    /**
     * 使用 HTTP 原始 query/form 参数计算代收异步通知签名。
     *
     * @param timestamp Header `t`
     * @param params 原始 query/form 参数首值 Map
     * @return SHA-256 hex 小写签名
     */
    public String sign(String timestamp, Map<String, String> params) {
        String signSource = buildSignSource(timestamp, params);
        return sha256Hex(signSource);
    }

    /**
     * 构建代收异步通知签名原文。
     *
     * 该方法只做字符串拼接，不写日志、不访问配置、不修改资金或交易状态。
     *
     * @param timestamp Header `t`
     * @param request 代收回调参数
     * @return 签名原文
     */
    public String buildSignSource(String timestamp, PayinWebhookRequest request) {
        return StringUtils.defaultString(timestamp)
                + StringUtils.defaultString(request.getTradeNo())
                + StringUtils.defaultString(request.getOrderNo())
                + StringUtils.defaultString(request.getCurrency())
                + amountText(request.getAmount())
                + (request.getStatus() == null ? "" : request.getStatus())
                + StringUtils.defaultString(request.getCode())
                + StringUtils.defaultString(request.getMessage());
    }

    /**
     * 使用 HTTP 原始 query/form 参数构建代收异步通知签名原文。
     *
     * 当前网关代收回调签名规则为：
     * t + tradeNo + orderNo + currency + amount + status + code + message。
     *
     * @param timestamp Header `t`
     * @param params 原始 query/form 参数首值 Map
     * @return 签名原文
     */
    public String buildSignSource(String timestamp, Map<String, String> params) {
        return StringUtils.defaultString(timestamp)
                + param(params, "tradeNo")
                + param(params, "orderNo")
                + param(params, "currency")
                + param(params, "amount")
                + param(params, "status")
                + param(params, "code")
                + param(params, "message");
    }

    /**
     * 将金额转换为签名使用的稳定字符串。
     *
     * @param amount 回调金额
     * @return 去除尾随 0 后的普通十进制文本
     */
    private String amountText(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    private String param(Map<String, String> params, String name) {
        return StringUtils.defaultString(params.get(name));
    }

    /**
     * 计算 SHA-256 hex。
     *
     * @param value 签名原文
     * @return SHA-256 hex 小写摘要
     */
    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
