package com.scott.payment.sdk.model.customer;

import lombok.Data;
import lombok.ToString;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerResponse
 * @date : 2026-07-02 18:36
 * @email : scott-***@163.com
 * @description : 客户响应模型，负责承载网关创建、更新、检索和列表接口返回的客户资料。
 *                响应可能包含姓名、邮箱、电话、证件号和地址等敏感个人信息，SDK 示例日志会做脱敏；本类不负责商户本地客户状态流转、
 *                KYC 结果判断、外部渠道同步或交易资金处理。
 * @status : modify
 */
@Data
public class CustomerResponse {

    /**
     * 客户 ID。
     *
     * 是否敏感：否。
     * 用途：用于后续检索、更新、删除客户接口的路径参数。
     */
    private String customerId;
    /**
     * 名。
     *
     * 是否敏感：是，属于客户个人信息。
     */
    private String firstname;
    /**
     * 姓。
     *
     * 是否敏感：是，属于客户个人信息。
     */
    private String lastname;
    /**
     * 邮箱地址。
     *
     * 是否敏感：是。
     */
    private String email;
    /**
     * 联系电话。
     *
     * 是否敏感：是。
     */
    private String phone;
    /**
     * 证件类型。
     *
     * 是否敏感：是。
     */
    private String identityType;

    /**
     * 证件号，禁止进入日志。
     *
     * 是否敏感：是。
     * 限制：不得输出到普通日志或前端。
     */
    @ToString.Exclude
    private String identityNo;

    /**
     * 国家代码。
     *
     * 格式：ISO 3166-1 alpha-2，例如 US。
     */
    private String country;
    /**
     * 州或省。
     */
    private String state;
    /**
     * 城市。
     */
    private String city;
    /**
     * 地址。
     */
    private String address;
    /**
     * 邮政编码。
     */
    private String zipcode;
    /**
     * 创建时间。
     *
     * 格式：以网关响应为准，可能为时间戳或日期时间字符串。
     */
    private String createTime;
}
