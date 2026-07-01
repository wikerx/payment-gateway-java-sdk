package com.scott.payment.sdk.client;

import com.scott.payment.sdk.OpenApiResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiResultTest
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI 通用响应模型测试，负责验证 code 判定和 message 兼容访问方法。
 *                本测试不涉及加密、HTTP 请求、资金状态流转或外部渠道调用。
 * @status : create
 */
class OpenApiResultTest {

    /**
     * 验证 code=0 时响应判定为成功。
     */
    @Test
    void isSuccessShouldUseZeroCode() {
        OpenApiResult<String> result = new OpenApiResult<String>();
        result.setCode(0);
        result.setMsg("ok");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("ok");
    }
}
