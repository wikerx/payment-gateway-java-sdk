package com.scott.payment.sdk.model.common;

import lombok.Data;

/**
 * 付款设备环境资料。
 */
@Data
public class DeviceInfo {

    /**
     * 客户端 IP。
     */
    private String ip;
    /**
     * 浏览器或客户端 User-Agent。
     */
    private String userAgent;
    /**
     * 浏览器 Accept-Language。
     */
    private String acceptLanguage;
    /**
     * 屏幕分辨率。
     */
    private String screenResolution;
    /**
     * 客户端时区。
     */
    private String timeZone;
    /**
     * 设备 ID。
     */
    private String deviceId;
}
