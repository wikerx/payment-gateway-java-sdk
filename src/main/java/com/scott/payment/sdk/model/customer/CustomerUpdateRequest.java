package com.scott.payment.sdk.model.customer;

import com.scott.payment.sdk.model.common.ShippingInfo;
import lombok.Data;
import lombok.ToString;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerUpdateRequest
 * @date : 2026-07-02 18:36
 * @email : scott-***@163.com
 * @description : 客户更新请求模型，负责承载商户更新客户资料时提交的姓名、邮箱、电话、证件、地址和收货地址资料。
 *                客户 ID 通过接口路径传入，本请求体不承载 customerId；请求包含个人信息和证件号等敏感字段，SDK 会加密 data 并脱敏日志。
 *                本类不负责客户幂等、KYC、状态流转、交易资金处理或外部渠道同步。
 * @status : create
 */
@Data
public class CustomerUpdateRequest {

    /**
     * 名。
     *
     * 是否允许为空：否。
     * 是否敏感：是，属于客户个人信息。
     */
    private String firstname;

    /**
     * 姓。
     *
     * 是否允许为空：否。
     * 是否敏感：是，属于客户个人信息。
     */
    private String lastname;

    /**
     * 邮箱地址。
     *
     * 是否允许为空：否。
     * 是否敏感：是。
     */
    private String email;

    /**
     * 联系电话。
     *
     * 是否允许为空：是。
     * 是否敏感：是。
     */
    private String phone;

    /**
     * 证件类型。
     *
     * 是否允许为空：是。
     * 是否敏感：是。
     */
    private String identityType;

    /**
     * 证件号，禁止进入日志。
     *
     * 是否允许为空：是。
     * 是否敏感：是。
     * 限制：仅用于加密请求，不得输出到普通日志。
     */
    @ToString.Exclude
    private String identityNo;

    /**
     * 国家代码。
     *
     * 是否允许为空：否。
     * 格式：ISO 3166-1 alpha-2，例如 US。
     */
    private String country;

    /**
     * 州或省。
     *
     * 是否允许为空：是。
     */
    private String state;

    /**
     * 城市。
     *
     * 是否允许为空：是。
     */
    private String city;

    /**
     * 地址。
     *
     * 是否允许为空：是。
     * 是否敏感：是。
     */
    private String address;

    /**
     * 邮政编码。
     *
     * 是否允许为空：是。
     */
    private String zipcode;

    /**
     * 收货地址。
     *
     * 是否允许为空：是。
     * 是否敏感：可能包含姓名、电话和地址。
     */
    private ShippingInfo shipping;
}
