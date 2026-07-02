package com.scott.payment.sdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiSdkApplication
 * @date : 2026-07-02 10:28
 * @email : scott_x@163.com
 * @description : OpenAPI SDK Spring Boot 示例启动类，负责在商户需要本地运行 webhook 接收服务时启动 Spring MVC 容器。
 *                本类不参与 SDK 普通依赖调用，不执行支付、代付、退款、资金计算、密钥轮换或交易状态流转。
 *                商户将 SDK 作为普通 Jar 使用时无需启动本类；只有需要直接运行回调接收示例时才使用 main 方法。
 * @status : create
 */
@SpringBootApplication
public class OpenApiSdkApplication {

    /**
     * 启动 OpenAPI SDK Spring Boot 示例应用。
     *
     * 该方法只启动 Web 容器和 SDK 示例 Controller，不会主动发起网关请求、修改资金或交易状态。
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OpenApiSdkApplication.class, args);
    }
}
