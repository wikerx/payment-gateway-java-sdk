package com.wikerx.payment.gateway.sdk.model.common;

import lombok.Data;
import lombok.ToString;

/**
 * 商户客户资料。
 */
@Data
public class CustomerInfo {

    /**
     * 客户 ID。
     */
    private String customerId;
    /**
     * 名。
     */
    private String firstname;
    /**
     * 姓。
     */
    private String lastname;
    /**
     * 姓名。
     */
    private String name;
    /**
     * 邮箱地址。
     */
    private String email;
    /**
     * 联系电话。
     */
    private String phone;
    /**
     * 证件类型。
     */
    private String identityType;

    /**
     * 证件号，禁止进入日志。
     */
    @ToString.Exclude
    private String identityNo;

    /**
     * 国家代码。
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
     * 收货地址。
     */
    private ShippingInfo shipping;
}
