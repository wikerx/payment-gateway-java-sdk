package com.scott.payment.sdk.model.refund;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : RefundResponse
 * @date : 2026-07-02 14:18
 * @email : scott_x@163.com
 * @description : 退款响应模型，负责承载网关返回的退款交易标识、原交易标识、退款金额、退款原因和状态信息。
 *                本类只做响应字段承载和状态枚举映射，不执行响应解密、退款资金处理、回调处理、幂等落库或交易状态推进。
 *                amount/refundAmount 涉及资金金额，charge/tradeNo/orderNo 是退款查询和对账关键字段，商户应结合查询或回调确认最终状态。
 * @status : modify
 */
@Data
public class RefundResponse {

    /**
     * 平台商户号。
     */
    private String merNo;
    /**
     * 平台交易流水号。
     */
    private String tradeNo;
    /**
     * 商户订单号。
     */
    private String orderNo;
    /**
     * 交易或账户币种，使用 ISO 4217 三位大写币种代码。
     */
    private String currency;
    /**
     * 金额，使用 BigDecimal 表示主币种单位。
     */
    private BigDecimal amount;
    /**
     * 支付方式。
     */
    private String paymentMethod;
    /**
     * 退款标识符。
     */
    private String charge;
    /**
     * 退款金额。
     */
    private BigDecimal refundAmount;
    /**
     * 退款原因。
     */
    private String refundReason;
    /**
     * 交易状态。
     */
    private Integer status;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;

    /**
     * 获取退款交易状态枚举。
     *
     * 该方法只根据响应 status 做 SDK 本地映射，不访问网关、不修改资金或交易状态；未知状态返回 UNKNOWN，方便商户兼容新增状态。
     *
     * @return 退款交易状态枚举
     */
    @JsonIgnore
    public RefundTradeStatus getStatusEnum() {
        return RefundTradeStatus.fromStatus(status);
    }

    /**
     * 获取退款交易状态说明。
     *
     * 该方法用于商户联调日志展示，不参与签名、加密、对账或状态流转。
     *
     * @return 状态说明
     */
    @JsonIgnore
    public String getStatusDescription() {
        return getStatusEnum().getMessage();
    }
}
