package com.scott.payment.sdk.model.common;

import lombok.Data;

/**
 * 收货地址资料。
 */
@Data
public class ShippingInfo {

    /**
     * 名。
     */
    private String firstname;
    /**
     * 姓。
     */
    private String lastname;
    /**
     * 联系电话。
     */
    private String phone;
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
}
