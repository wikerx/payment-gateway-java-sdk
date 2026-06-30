package com.wikerx.payment.gateway.sdk.model.customer;

import com.wikerx.payment.gateway.sdk.model.common.ShippingInfo;
import lombok.Data;
import lombok.ToString;

/**
 * 客户创建请求。
 */
@Data
public class CustomerCreateRequest {

    /**
     * 名。
     */
    private String firstname;
    /**
     * 姓。
     */
    private String lastname;
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
