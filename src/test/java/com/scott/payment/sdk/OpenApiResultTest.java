package com.scott.payment.sdk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiResultTest
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI 通用响应模型测试，负责验证 code 判定和 message 兼容访问方法。
 *                本测试只验证 SDK 本地响应包装对象，不发起真实 HTTP 请求、不模拟网关成功响应、不执行签名加密、
 *                不创建支付、代付、退款、客户或资金交易。
 * @status : modify
 */
class OpenApiResultTest {

    /**
     * 验证 code=0 时响应判定为成功。
     *
     * 本方法只构造本地 OpenApiResult 对象，不访问网关、不修改资金或交易状态。
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
