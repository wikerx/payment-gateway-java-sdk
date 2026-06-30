package com.wikerx.payment.gateway.sdk.client;

import com.wikerx.payment.gateway.sdk.PaymentGatewayResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentGatewayResultTest {

    /**
     * 验证 code=0 时响应判定为成功。
     */
    @Test
    void isSuccessShouldUseZeroCode() {
        PaymentGatewayResult<String> result = new PaymentGatewayResult<String>();
        result.setCode(0);
        result.setMsg("ok");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("ok");
    }
}
