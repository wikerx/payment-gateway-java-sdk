package com.scott.payment.sdk.json;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : JsonSupportTest
 * @date : 2026-07-02 16:52
 * @email : scott_x@163.com
 * @description : SDK JSON 序列化测试，负责验证商户联调日志中的金额字段不会输出科学计数法。
 *                本测试不发起 HTTP 请求、不执行 OpenAPI 加密、不修改资金状态，只锁定 BigDecimal 日志展示格式。
 * @status : create
 */
public class JsonSupportTest {

    /**
     * 验证普通 JSON 和日志 JSON 都使用普通数字格式输出 BigDecimal。
     *
     * 金额日志需要方便商户直接核对网关返回值，不能输出 1.101E+6 这类科学计数法。
     */
    @Test
    public void testBigDecimalSerializeAsPlainString() {
        Map<String, Object> value = new LinkedHashMap<String, Object>();
        value.put("balance", new BigDecimal("1.101E+6"));
        value.put("frozenAmounts", new BigDecimal("0"));

        assertThat(JsonSupport.toJson(value)).contains("\"balance\":1101000");
        assertThat(JsonSupport.toLogJson(value)).contains("\"balance\":1101000");
        assertThat(JsonSupport.toJson(value)).doesNotContain("1.101E+6");
        assertThat(JsonSupport.toLogJson(value)).doesNotContain("1.101E+6");
    }
}
