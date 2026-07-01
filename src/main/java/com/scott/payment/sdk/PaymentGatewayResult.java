package com.scott.payment.sdk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Payment Gateway 通用响应模型，对齐后端 code、msg、data、livemode 结构。
 *
 * @param <T> 业务响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentGatewayResult<T> {

    /**
     * 业务响应码，0 表示成功。
     */
    private Integer code;

    /**
     * 响应说明，失败时为商户可理解的错误信息。
     */
    private String msg;

    /**
     * 响应数据，成功响应会由 SDK 解密并反序列化为目标类型。
     */
    private T data;

    /**
     * 是否生产模式。
     */
    private Boolean livemode;

    /**
     * 判断响应码是否表示成功。
     *
     * @return true 表示响应成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return Integer.valueOf(0).equals(code);
    }

    /**
     * 兼容商户习惯的 message 命名，实际映射字段为 msg。
     *
     * @return 响应说明
     */
    @JsonIgnore
    public String getMessage() {
        return msg;
    }
}
