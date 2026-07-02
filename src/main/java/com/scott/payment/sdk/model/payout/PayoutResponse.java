package com.scott.payment.sdk.model.payout;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PayoutResponse
 * @date : 2026-07-02 14:18
 * @email : scott_x@163.com
 * @description : 代付交易响应模型，负责承载网关返回的代付交易标识、金额、支付方式、完成时间和状态信息。
 *                本类只做响应字段承载和状态枚举映射，不执行响应解密、出款、渠道回调处理、幂等落库或交易状态推进。
 *                amount 涉及资金金额，tradeNo/orderNo 是商户对账关键字段，日志输出应保证可追踪但不得混淆生产资金结果。
 * @status : modify
 */
@Data
public class PayoutResponse {

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
     * 完成时间。
     */
    private String completionDate;
    /**
     * 交易状态。
     */
    private Integer status;
    /**
     * 业务状态码。
     */
    private String code;
    /**
     * 业务状态说明。
     */
    private String message;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;

    /**
     * 获取代付交易状态枚举。
     *
     * 该方法只根据响应 status 做 SDK 本地映射，不访问网关、不修改资金或交易状态；未知状态返回 UNKNOWN，方便商户兼容新增状态。
     *
     * @return 代付交易状态枚举
     */
    @JsonIgnore
    public PayoutTradeStatus getStatusEnum() {
        return PayoutTradeStatus.fromStatus(status);
    }

    /**
     * 获取代付交易状态说明。
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
