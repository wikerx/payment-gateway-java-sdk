package com.scott.payment.sdk.model;

import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.model.common.PaymentMethod;
import com.scott.payment.sdk.model.payment.PaymentCreateRequest;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : PaymentMethodTest
 * @date : 2026-07-01 17:43
 * @email : scott_x@163.com
 * @description : 支付方式枚举测试，负责验证 SDK 暴露给商户的 paymentMethod 枚举值、解析逻辑和请求模型序列化兼容性。
 *                本测试不发起 HTTP 请求，不执行签名、加密、资金计算、状态流转或外部渠道调用。
 * @status : create
 */
class PaymentMethodTest {

    /**
     * 验证支付方式枚举值与网关文档代码一致。
     */
    @Test
    void paymentMethod_shouldExposeGatewayCodes() {
        assertThat(PaymentMethod.CARD.getCode()).isEqualTo("CARD");
        assertThat(PaymentMethod.PAY_PAL.getCode()).isEqualTo("PAY_PAL");
        assertThat(PaymentMethod.CASHAPP.getCode()).isEqualTo("CASHAPP");
        assertThat(PaymentMethod.ACH_DEBIT.getCode()).isEqualTo("ACH_DEBIT");
        assertThat(PaymentMethod.UPI.getCode()).isEqualTo("UPI");
    }

    /**
     * 验证可按网关支付方式代码解析枚举。
     */
    @Test
    void fromCode_shouldParseGatewayCode() {
        assertThat(PaymentMethod.fromCode("CARD")).isEqualTo(PaymentMethod.CARD);
        assertThat(PaymentMethod.fromCode(" pay_pal ")).isEqualTo(PaymentMethod.PAY_PAL);
        assertThat(PaymentMethod.fromCode("cashapp")).isEqualTo(PaymentMethod.CASHAPP);
        assertThat(PaymentMethod.fromCode("ACH_DEBIT")).isEqualTo(PaymentMethod.ACH_DEBIT);
        assertThat(PaymentMethod.fromCode("upi")).isEqualTo(PaymentMethod.UPI);
    }

    /**
     * 验证非法支付方式代码会快速失败。
     */
    @Test
    void fromCode_withInvalidCode_shouldThrowException() {
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                PaymentMethod.fromCode("UNKNOWN");
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported paymentMethod code");
    }

    /**
     * 验证空支付方式代码会快速失败。
     */
    @Test
    void fromCode_withBlankCode_shouldThrowException() {
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                PaymentMethod.fromCode(" ");
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentMethod code can not be blank");
    }

    /**
     * 验证代收请求使用枚举 setter 后仍按字符串序列化 paymentMethod。
     */
    @Test
    void paymentCreateRequest_shouldSerializePaymentMethodCode() {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setPaymentMethod(PaymentMethod.CARD);

        assertThat(request.getPaymentMethod()).isEqualTo("CARD");
        assertThat(JsonSupport.toJson(request)).contains("\"paymentMethod\":\"CARD\"");
    }

    /**
     * 验证代付请求使用枚举 setter 后仍按字符串序列化 paymentMethod。
     */
    @Test
    void payoutCreateRequest_shouldSerializePaymentMethodCode() {
        PayoutCreateRequest request = new PayoutCreateRequest();
        request.setPaymentMethod(PaymentMethod.PAY_PAL);

        assertThat(request.getPaymentMethod()).isEqualTo("PAY_PAL");
        assertThat(JsonSupport.toJson(request)).contains("\"paymentMethod\":\"PAY_PAL\"");
    }

    /**
     * 验证请求模型的枚举 setter 不接受空枚举，避免商户误传 null 后生成无效请求。
     */
    @Test
    void requestSetPaymentMethod_withNullEnum_shouldThrowException() {
        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                new PaymentCreateRequest().setPaymentMethod((PaymentMethod) null);
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentMethod can not be null");

        assertThatThrownBy(new org.assertj.core.api.ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() {
                new PayoutCreateRequest().setPaymentMethod((PaymentMethod) null);
            }
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paymentMethod can not be null");
    }
}
