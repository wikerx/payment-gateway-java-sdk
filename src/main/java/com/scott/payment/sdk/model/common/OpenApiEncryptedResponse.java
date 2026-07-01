package com.scott.payment.sdk.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiEncryptedResponse
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI 统一密文响应外壳，负责承载网关响应 code、msg、livemode 和加密 data。
 *                本类不表示最终业务响应 DTO，不执行响应解密，也不修改支付、退款、代付或资金状态；SDK 会先解析该外壳，再解密 data 并转换为业务响应对象。
 * @status : create
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenApiEncryptedResponse {

    /**
     * 响应码，0 表示成功。
     *
     * 是否允许为空：否。
     * 用途：商户侧判断网关处理结果。
     */
    private Integer code;

    /**
     * 响应信息。
     *
     * 是否允许为空：允许为空字符串。
     * 用途：失败时返回商户可理解的错误说明。
     */
    private String msg;

    /**
     * 响应所属环境。
     *
     * 是否允许为空：允许为空，非空时 SDK 会与本地配置校验一致性。
     * 用途：避免测试环境和生产环境数据串用。
     */
    private Boolean livemode;

    /**
     * 加密后的响应 data，格式为 protectedHeader.encryptedAesKey.iv.cipherText.tag。
     *
     * 敏感字段：是。
     * 是否允许为空：允许为空，部分失败响应可能不返回业务 data。
     * 用途：承载业务响应 DTO 加密后的密文。
     * 限制：默认日志只输出摘要，完整值仅在调试日志开关开启后用于沙盒联调。
     */
    @ToString.Exclude
    private String data;
}
