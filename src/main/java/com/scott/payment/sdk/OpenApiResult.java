package com.scott.payment.sdk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiResult
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI 通用响应模型，负责向商户返回业务 code、msg、解密后的 data 和 livemode。
 *                本类不承载密文 data，不执行响应解密，不修改交易、退款、代付或资金状态；状态流转和幂等仍由网关服务端负责。
 *                data 可能包含交易、客户或余额信息，商户输出日志前需要按自身安全规范处理。
 * @status : create
 *
 * @param <T> 业务响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenApiResult<T> {

    /**
     * 业务响应码，0 表示成功。
     *
     * 是否允许为空：否。
     * 用途：商户侧判断 API 业务处理结果。
     */
    private Integer code;

    /**
     * 响应说明，失败时为商户可理解的错误信息。
     *
     * 是否允许为空：允许为空字符串。
     * 用途：错误排查和商户页面提示。
     */
    private String msg;

    /**
     * 响应数据，成功响应会由 SDK 解密并反序列化为目标类型。
     *
     * 是否允许为空：允许为空。
     * 用途：承载交易、退款、代付、余额或客户响应对象。
     * 限制：可能包含金额、状态、客户资料等业务数据，商户日志需自行控制。
     */
    private T data;

    /**
     * 是否生产模式。
     *
     * 是否允许为空：允许为空，服务端返回时 SDK 会优先校验非空值。
     * 用途：确认响应数据所属环境，避免测试和生产数据串用。
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
