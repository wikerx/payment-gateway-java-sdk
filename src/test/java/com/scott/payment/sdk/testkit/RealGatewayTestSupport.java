package com.scott.payment.sdk.testkit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : RealGatewayTestSupport
 * @date : 2026-07-02 11:38
 * @email : scott_x@163.com
 * @description : 真实网关联调用例辅助类，负责提供有序日志字段构造。
 *                本类不探测或跳过网关请求，不发起 OpenAPI 业务请求、不执行签名加密、不创建支付、退款或代付交易，也不修改资金和交易状态。
 *                真实业务调用仍由各个 payin、payout、refund case 显式完成，便于商户直接阅读接口参数。
 * @status : create
 */
public final class RealGatewayTestSupport {

    private RealGatewayTestSupport() {
    }

    /**
     * 构造有序日志字段。
     *
     * @param keyValues key/value 交替字段
     * @return 日志字段 Map
     */
    public static Map<String, Object> logFields(Object... keyValues) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            fields.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return fields;
    }
}
