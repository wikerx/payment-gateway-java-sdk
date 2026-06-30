package com.wikerx.payment.gateway.sdk.http;

/**
 * SDK HTTP 传输接口，用于生产环境调用和单元测试替换。
 */
public interface HttpTransport {

    /**
     * 执行 POST 请求。
     *
     * @param request SDK HTTP 请求
     * @return SDK HTTP 响应
     */
    SdkHttpResponse execute(SdkHttpRequest request);
}
