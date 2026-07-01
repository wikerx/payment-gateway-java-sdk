package com.scott.payment.sdk.model.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiEncryptedRequest
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : OpenAPI 统一密文请求外壳，负责承载 livemode 和加密后的 data 字段。
 *                本类不承载业务字段、不执行加密算法、不参与资金状态流转；data 为 compact JWE 结构，日志输出必须受调试开关和脱敏策略控制。
 * @status : create
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenApiEncryptedRequest {

    /**
     * 是否生产环境。
     *
     * false = 测试环境。
     * true = 生产环境。
     * 是否允许为空：否，由 SDK 配置写入。
     * 用途：参与网关环境隔离校验。
     */
    private Boolean livemode;

    /**
     * 加密后的请求 data，格式为 protectedHeader.encryptedAesKey.iv.cipherText.tag。
     *
     * 敏感字段：是。
     * 是否允许为空：POST 加密请求不允许为空。
     * 用途：承载业务请求 DTO 加密后的密文。
     * 限制：默认日志只输出摘要，完整值仅在调试日志开关开启后用于沙盒联调。
     */
    @ToString.Exclude
    private String data;
}
