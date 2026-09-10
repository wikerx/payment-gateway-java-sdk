package com.scott.payment.sdk.api.customers;

import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.config.MerchantConfigLoader;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : CustomerCreateTest
 * @date : 2026-07-02 18:36
 * @email : scott-***@163.com
 * @description : 创建客户接口真实网关调用 case，负责使用 merchant-config.properties 创建 SDK 客户端并向测试网关发起
 *                /pay-api/mer/customers 请求。本 case 会真实创建沙盒客户资料，涉及姓名、邮箱、电话、地址和证件号等敏感数据；
 *                SDK 会按最新 OpenAPI 协议完成 JWT 签名、请求加密、HTTP 调用和响应解密。本 case 不负责商户本地客户幂等、
 *                KYC、状态流转或外部渠道同步。
 * @status : create
 */
@Slf4j
public class CustomerCreateTest {

    /**
     * 真实请求测试环境网关创建客户。
     *
     * 该方法通过默认 JDK HTTP Transport 发起真实 POST 请求，重复运行会生成唯一邮箱和证件号，避免沙盒客户资料冲突。
     */
    @Test
    public void testCustomerCreate() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        CustomerCreateRequest request = CustomerApiTestSupport.createRequest();

        log.info("创建客户真实调用-请求原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
        OpenApiResult<CustomerResponse> result = client.createCustomer(request);
        log.info("创建客户真实调用-响应原始明文参数: {}",
                JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));

        assertThat(result).isNotNull();
        assertThat(result.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getCustomerId()).isNotBlank();
    }

    /**
     * 真实请求测试环境网关创建客户。
     *
     * 该方法通过默认 JDK HTTP Transport 发起真实 POST 请求，重复运行会生成唯一邮箱和证件号，避免沙盒客户资料冲突。
     */
    @Test
    public void testCustomerCreateTimes() {
        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);
        for (int i = 0; i < 999; i++) {
            CustomerCreateRequest request = CustomerApiTestSupport.createRequest();

            log.info("创建客户真实调用-请求原始明文参数: {}",
                    JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
            OpenApiResult<CustomerResponse> result = client.createCustomer(request);
            log.info("创建客户真实调用-响应原始明文参数: {}",
                    JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(result)));
        }

    }


    @Test
    public void testCustomerCreateThread() throws Exception {
        final int totalRequests = 999999;
        final int concurrentThreads = 200;

        OpenApiClientConfig config = MerchantConfigLoader.load();
        OpenApiClient client = new OpenApiClient(config);

        // 提前构造请求，避免随机数据生成库本身存在并发安全问题。
        List<CustomerCreateRequest> requests =
                new ArrayList<CustomerCreateRequest>(totalRequests);
        for (int i = 0; i < totalRequests; i++) {
            requests.add(CustomerApiTestSupport.createRequest());
        }

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Void>> futures =
                new ArrayList<Future<Void>>(totalRequests);

        long startMillis = System.currentTimeMillis();

        try {
            for (int i = 0; i < totalRequests; i++) {
                final int taskNo = i + 1;
                final CustomerCreateRequest request = requests.get(i);

                futures.add(executor.submit(() -> {
                    // 让第一批线程尽量同时开始。
                    startGate.await();

                    log.info("第 {} 次创建客户-请求: {}",
                            taskNo,
                            JsonSupport.toLogJson(
                                    OpenApiLogSanitizer.sanitizeObject(request)));

                    OpenApiResult<CustomerResponse> result =
                            client.createCustomer(request);

                    log.info("第 {} 次创建客户-响应: {}",
                            taskNo,
                            JsonSupport.toLogJson(
                                    OpenApiLogSanitizer.sanitizeObject(result)));

                    // 子线程中的异常必须通过 Future.get() 传回测试线程。
                    assertThat(result).isNotNull();
                    assertThat(result.getLivemode())
                            .isEqualTo(config.getLivemode());
                    assertThat(result.isSuccess()).isTrue();
                    assertThat(result.getData()).isNotNull();
                    assertThat(result.getData().getCustomerId()).isNotBlank();

                    return null;
                }));
            }

            executor.shutdown();
            startGate.countDown();

            // 等待全部请求结束；任何一个任务失败都会使测试失败。
            for (Future<Void> future : futures) {
                future.get();
            }

            long elapsedMillis = System.currentTimeMillis() - startMillis;
            log.info("客户创建并发测试完成，总请求数: {}, 并发数: {}, 耗时: {} ms",
                    totalRequests, concurrentThreads, elapsedMillis);
        } finally {
            // 提交过程发生异常时，释放已启动的线程。
            startGate.countDown();
            executor.shutdownNow();
        }
    }



}
