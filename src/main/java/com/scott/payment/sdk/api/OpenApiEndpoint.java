package com.scott.payment.sdk.api;

import com.scott.payment.sdk.config.OpenApiConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiEndpoint
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : 商户 OpenAPI 元数据枚举，负责集中维护 SDK 已集成接口的 API 名称、HTTP 方法和路径模板。
 *                本枚举不负责请求加密、响应解密、资金状态流转或幂等控制；路径常量仍由 OpenApiConstants 维护，避免重复定义接口地址。
 * @status : create
 */
@Getter
@AllArgsConstructor
public enum OpenApiEndpoint {

    /**
     * 创建代收交易，POST 请求，业务请求体需要加密。
     */
    PAYMENT_CREATE("Payment Create", "POST", OpenApiConstants.PAYMENT_CREATE_PATH),

    /**
     * 检索代收交易，GET 请求，无请求体，响应 data 需要解密。
     */
    PAYMENT_RETRIEVE("Payment Retrieve", "GET", OpenApiConstants.PAYMENT_RETRIEVE_PATH),

    /**
     * 创建退款，POST 请求，涉及资金反向申请，业务请求体需要加密。
     */
    REFUND_CREATE("Refund Create", "POST", OpenApiConstants.REFUND_CREATE_PATH),

    /**
     * 检索退款，GET 请求，无请求体，响应 data 需要解密。
     */
    REFUND_RETRIEVE("Refund Retrieve", "GET", OpenApiConstants.REFUND_RETRIEVE_PATH),

    /**
     * 创建代付交易，POST 请求，涉及出款申请和收款人信息，业务请求体需要加密。
     */
    PAYOUT_TRANSFER_CREATE("Payout Transfer Create", "POST", OpenApiConstants.PAYOUT_CREATE_PATH),

    /**
     * 检索代付交易，GET 请求，无请求体，响应 data 需要解密。
     */
    PAYOUT_TRANSFER_RETRIEVE("Payout Transfer Retrieve", "GET", OpenApiConstants.PAYOUT_RETRIEVE_PATH),

    /**
     * 取消代付交易，POST 请求，涉及代付状态变更申请，业务请求体需要加密。
     */
    PAYOUT_TRANSFER_CANCEL("Payout Transfer Cancel", "POST", OpenApiConstants.PAYOUT_CANCEL_PATH),

    /**
     * 检索资金账户余额，GET 请求，无请求体，响应 data 需要解密为余额列表。
     */
    FUND_ACCOUNTS_BALANCE_INQUIRY("Fund Accounts Balance Inquiry", "GET", OpenApiConstants.BALANCE_RETRIEVE_PATH),

    /**
     * 创建客户，POST 请求，涉及客户资料，业务请求体需要加密。
     */
    CUSTOMER_CREATE("Customer Create", "POST", OpenApiConstants.CUSTOMER_CREATE_PATH),

    /**
     * 检索客户，GET 请求，无请求体，响应 data 需要解密。
     */
    CUSTOMER_RETRIEVE("Customer Retrieve", "GET", OpenApiConstants.CUSTOMER_RETRIEVE_PATH);

    /**
     * API 展示名称，用于日志、测试 caseName 和商户文档。
     */
    private final String apiName;

    /**
     * HTTP 方法，当前支持 GET 和 POST。
     */
    private final String method;

    /**
     * API 路径或路径模板，不包含 base-url。
     */
    private final String path;

    /**
     * 使用路径参数格式化 API 路径。
     *
     * @param args 路径模板参数，调用方负责先完成 URL path 编码
     * @return 完整 API 路径，不包含 base-url
     */
    public String formatPath(Object... args) {
        if (args == null || args.length == 0) {
            return path;
        }
        return String.format(path, args);
    }
}
