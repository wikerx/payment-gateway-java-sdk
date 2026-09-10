package com.scott.payment.sdk.model;

import com.scott.payment.sdk.model.common.PaymentType;
import com.scott.payment.sdk.model.payment.PaymentTradeStatus;
import com.scott.payment.sdk.model.payout.PayoutTradeStatus;
import com.scott.payment.sdk.model.refund.RefundTradeStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : TradeStatusEnumTest
 * @date : 2026-07-02 14:18
 * @email : scott-***@163.com
 * @description : SDK 交易状态和支付类型枚举测试，负责验证商户示例中常用的 payType 与响应 status 映射关系。
 *                本测试不发起真实 HTTP 请求、不执行签名加密、不创建支付、代付或退款交易，也不修改资金和交易状态。
 * @status : create
 */
public class TradeStatusEnumTest {

    /**
     * 验证本地支付直连 payType 枚举值。
     *
     * 本测试只校验 SDK 本地枚举映射，不访问网关、不修改交易或资金状态。
     */
    @Test
    public void testPaymentTypeLocalCode() {
        assertThat(PaymentType.Direct.getCode()).isEqualTo(1);
        assertThat(PaymentType.fromCode(1)).isEqualTo(PaymentType.Direct);
    }

    /**
     * 验证代收响应 status 映射。
     *
     * 本测试只校验 SDK 本地枚举映射，不访问网关、不修改交易或资金状态。
     */
    @Test
    public void testPaymentTradeStatusMapping() {
        assertThat(PaymentTradeStatus.fromStatus(0)).isEqualTo(PaymentTradeStatus.CREATED);
        assertThat(PaymentTradeStatus.fromStatus(2)).isEqualTo(PaymentTradeStatus.SUCCESS);
        assertThat(PaymentTradeStatus.fromStatus(999)).isEqualTo(PaymentTradeStatus.UNKNOWN);
        assertThat(PaymentTradeStatus.SUCCESS.isFinalStatus()).isTrue();
    }

    /**
     * 验证代付响应 status 映射。
     *
     * 本测试只校验 SDK 本地枚举映射，不访问网关、不修改交易或资金状态。
     */
    @Test
    public void testPayoutTradeStatusMapping() {
        assertThat(PayoutTradeStatus.fromStatus(0)).isEqualTo(PayoutTradeStatus.Reviewing);
        assertThat(PayoutTradeStatus.fromStatus(0).getDescription()).isEqualTo("审核中");
        assertThat(PayoutTradeStatus.fromStatus(1)).isEqualTo(PayoutTradeStatus.Processing);
        assertThat(PayoutTradeStatus.fromStatus(1).getDescription()).isEqualTo("处理中");
        assertThat(PayoutTradeStatus.fromStatus(2)).isEqualTo(PayoutTradeStatus.Succeeded);
        assertThat(PayoutTradeStatus.fromStatus(2).getDescription()).isEqualTo("处理成功");
        assertThat(PayoutTradeStatus.fromStatus(3)).isEqualTo(PayoutTradeStatus.Failed);
        assertThat(PayoutTradeStatus.fromStatus(3).getDescription()).isEqualTo("处理失败");
        assertThat(PayoutTradeStatus.fromStatus(4)).isEqualTo(PayoutTradeStatus.Cancelled);
        assertThat(PayoutTradeStatus.fromStatus(4).getDescription()).isEqualTo("已取消");
        assertThat(PayoutTradeStatus.fromStatus(999)).isEqualTo(PayoutTradeStatus.Unknown);
        assertThat(PayoutTradeStatus.Succeeded.isFinalStatus()).isTrue();
    }

    /**
     * 验证退款响应 status 映射。
     *
     * 本测试只校验 SDK 本地枚举映射，不访问网关、不修改交易或资金状态。
     */
    @Test
    public void testRefundTradeStatusMapping() {
        assertThat(RefundTradeStatus.fromStatus(-1)).isEqualTo(RefundTradeStatus.CREATED_ERROR);
        assertThat(RefundTradeStatus.fromStatus(2)).isEqualTo(RefundTradeStatus.SUCCESS);
        assertThat(RefundTradeStatus.fromStatus(999)).isEqualTo(RefundTradeStatus.UNKNOWN);
        assertThat(RefundTradeStatus.SUCCESS.isFinalStatus()).isTrue();
    }
}
