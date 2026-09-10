package com.scott.payment.sdk.model.refund;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : RefundTradeStatus
 * @date : 2026-07-02 14:18
 * @email : scott-***@163.com
 * @description : 退款交易状态枚举，负责把网关响应中的 status 数字映射为商户可读的状态码和状态说明。
 *                本枚举只用于 SDK 响应解析、日志展示和商户本地判断参考，不提交退款、不推进网关状态、不处理资金入账或对账、
 *                不替代商户系统的退款幂等和终态保护；最终退款结果应以查询接口或网关异步通知为准。
 * @status : create
 */
public enum RefundTradeStatus {

    /**
     * 创建异常。
     */
    CREATED_ERROR(-1, "error", "Error"),

    /**
     * 已创建，等待处理。
     */
    CREATED(0, "requires_action", "Pending"),

    /**
     * 处理中。
     */
    PAYING(1, "requires_action", "Processing"),

    /**
     * 退款成功。
     */
    SUCCESS(2, "succeeded", "Succeeded"),

    /**
     * 退款失败。
     */
    FAIL(3, "failed", "Failed"),

    /**
     * 当前 SDK 未识别的网关状态。
     */
    UNKNOWN(null, "unknown", "Unknown status");

    /**
     * 网关响应 status 数字。
     */
    private final Integer status;

    /**
     * 网关业务 code。
     */
    private final String code;

    /**
     * 网关业务消息。
     */
    private final String message;

    RefundTradeStatus(Integer status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /**
     * 获取网关响应 status 数字。
     *
     * @return status 数字；UNKNOWN 返回 null
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 获取网关业务 code。
     *
     * @return 业务状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取状态说明。
     *
     * @return 状态说明
     */
    public String getMessage() {
        return message;
    }

    /**
     * 根据网关响应 status 解析退款交易状态。
     *
     * 该方法只做本地映射，不访问网关、不修改资金、不推进交易状态；未知状态统一返回 UNKNOWN，便于商户兼容网关新增状态。
     *
     * @param status 网关响应 status
     * @return 退款交易状态枚举
     */
    public static RefundTradeStatus fromStatus(Integer status) {
        if (status == null) {
            return UNKNOWN;
        }
        for (RefundTradeStatus tradeStatus : values()) {
            if (status.equals(tradeStatus.status)) {
                return tradeStatus;
            }
        }
        return UNKNOWN;
    }

    /**
     * 判断是否为终态。
     *
     * 本方法只用于商户本地展示或判断参考，不替代商户系统自己的幂等和终态保护。
     *
     * @return true 表示成功或失败等终态
     */
    public boolean isFinalStatus() {
        return SUCCESS.equals(this) || FAIL.equals(this);
    }
}
