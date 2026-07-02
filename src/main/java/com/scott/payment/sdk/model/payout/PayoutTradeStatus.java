package com.scott.payment.sdk.model.payout;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutTradeStatus
 * @date : 2026-07-02 14:18
 * @email : scott_x@163.com
 * @description : 代付交易状态枚举，负责把网关响应中的 status 数字映射为商户可读的状态码和状态说明。
 *                本枚举只用于 SDK 响应解析、日志展示和商户本地判断参考，不发起出款、不推进网关状态、不处理渠道回调、
 *                不替代商户系统的幂等、终态保护或资金对账；最终出款结果应以查询接口或网关异步通知为准。
 * @status : create
 */
public enum PayoutTradeStatus {

    /**
     * 审核中，网关已接收代付申请并等待审核或后续处理。
     */
    Reviewing(0, "requires_action", "Req successfully", "审核中"),

    /**
     * 处理中，兼容网关在异步处理阶段返回的中间状态。
     */
    Processing(1, "processing", "Processing", "处理中"),

    /**
     * 代付处理成功，通常表示测试环境已完成出款流程。
     */
    Succeeded(2, "succeeded", "Succeeded", "处理成功"),

    /**
     * 代付处理失败。
     */
    Failed(3, "failed", "Failed", "处理失败"),

    /**
     * 已取消。
     */
    Cancelled(4, "canceled", "Cancelled", "已取消"),

    /**
     * 当前 SDK 未识别的网关状态。
     */
    Unknown(null, "unknown", "Unknown status", "未知状态");

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

    /**
     * SDK 对代付交易 status 的中文业务解释。
     */
    private final String description;

    PayoutTradeStatus(Integer status, String code, String message, String description) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.description = description;
    }

    /**
     * 获取网关响应 status 数字。
     *
     * @return status 数字；Unknown 返回 null
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
     * 获取 SDK 对代付交易 status 的中文业务解释。
     *
     * 本说明用于商户联调日志和示例代码展示，不替代网关原始 code/message，也不推进商户本地状态机。
     *
     * @return 中文业务解释
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据网关响应 status 解析代付交易状态。
     *
     * 该方法只做本地映射，不访问网关、不修改资金、不推进交易状态；未知状态统一返回 UNKNOWN，便于商户兼容网关新增状态。
     *
     * @param status 网关响应 status
     * @return 代付交易状态枚举
     */
    public static PayoutTradeStatus fromStatus(Integer status) {
        if (status == null) {
            return Unknown;
        }
        for (PayoutTradeStatus tradeStatus : values()) {
            if (status.equals(tradeStatus.status)) {
                return tradeStatus;
            }
        }
        return Unknown;
    }

    /**
     * 判断是否为终态。
     *
     * 本方法只用于商户本地展示或判断参考，不替代商户系统自己的幂等和终态保护。
     *
     * @return true 表示成功、失败或取消等终态
     */
    public boolean isFinalStatus() {
        return Succeeded.equals(this) || Failed.equals(this) || Cancelled.equals(this);
    }
}
