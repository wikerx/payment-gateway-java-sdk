package com.wikerx.payment.gateway.sdk.model.payout;

import lombok.Data;

/**
 * 代付取消请求。
 */
@Data
public class PayoutCancelRequest {

    /**
     * 平台交易流水号。
     */
    private String tradeNo;
    /**
     * 商户订单号。
     */
    private String orderNo;
    /**
     * 备注。
     */
    private String remark;
}
