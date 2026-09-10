package com.scott.payment.sdk.model.refund;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : RefundCreateRequest
 * @date : 2026-07-02 15:34
 * @email : scott-***@163.com
 * @description : 代收退款申请请求模型，负责承载商户退款订单号、原代收交易号、币种、原交易金额、退款金额和退款原因。
 *                本类只描述请求字段，不执行 JWT 签名、OpenAPI 报文加密、HTTP 调用、退款幂等落库、资金回退或交易状态流转。
 *                orderNo 是商户退款申请幂等和对账关键字段，tradeNo/charge 用于定位原代收交易，amount/refundAmount 涉及资金金额。
 * @status : modify
 */
@Data
public class RefundCreateRequest {

    /**
     * 退款标识符，通常用于查询已经创建的退款。
     */
    private String charge;
    /**
     * 平台交易流水号。
     */
    private String tradeNo;
    /**
     * 交易或账户币种，使用 ISO 4217 三位大写币种代码。
     */
    private String currency;
    /**
     * 金额，使用 BigDecimal 表示主币种单位。
     */
    private BigDecimal amount;
    /**
     * 退款金额。
     */
    private BigDecimal refundAmount;
    /**
     * 退款原因。
     */
    private String refundReason;
    /**
     * 商户透传字段。
     */
    private String metadata;
    /**
     * 备注。
     */
    private String remark;
}
