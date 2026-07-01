package com.scott.payment.sdk.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OrderNoGeneratorTest
 * @date : 2026-07-01 16:42
 * @email : scott_x@163.com
 * @description : 本地订单号生成工具测试，负责验证 SDK 示例订单号在单次、连续和多线程场景下的格式与单 JVM 不重复约束。
 *                本测试不访问数据库、Redis 或外部服务，不发起支付、代付、退款请求，不修改资金、密钥或配置状态。
 * @status : create
 */
class OrderNoGeneratorTest {

    /**
     * 验证默认生成方法返回非空订单号。
     */
    @Test
    void generate_shouldReturnNotBlankOrderNo() {
        String orderNo = OrderNoGenerator.generate();

        assertThat(orderNo).isNotBlank();
        assertThat(orderNo).matches("\\d{20}");
    }

    /**
     * 验证带前缀生成格式符合 prefix + yyyyMMddHHmmssSSS + seq。
     */
    @Test
    void generate_withPrefix_shouldMatchFormat() {
        String orderNo = OrderNoGenerator.generate("PAY");

        assertThat(orderNo).matches("PAY\\d{20}");
        assertThat(orderNo).startsWith("PAY");
    }

    /**
     * 验证连续生成 10000 个订单号不重复。
     */
    @Test
    void generate_continuously_shouldNotDuplicate() {
        Set<String> orderNos = new HashSet<String>();

        for (int index = 0; index < 10000; index++) {
            orderNos.add(OrderNoGenerator.generate("PAY"));
        }

        assertThat(orderNos).hasSize(10000);
    }

    /**
     * 验证多线程并发生成订单号不重复。
     *
     * @throws Exception 并发任务等待被中断时抛出
     */
    @Test
    void generate_concurrently_shouldNotDuplicate() throws Exception {
        int threadCount = 16;
        int perThreadCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        Set<String> orderNos = Collections.synchronizedSet(new HashSet<String>());

        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ready.countDown();
                    try {
                        start.await();
                        for (int index = 0; index < perThreadCount; index++) {
                            orderNos.add(OrderNoGenerator.generate("PAY"));
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executorService.shutdownNow();

        assertThat(orderNos).hasSize(threadCount * perThreadCount);
    }

    /**
     * 验证 prefix 为空或 null 时仍正常生成。
     */
    @Test
    void generate_blankPrefix_shouldReturnTimeBasedOrderNo() {
        assertThat(OrderNoGenerator.generate(null)).matches("\\d{20}");
        assertThat(OrderNoGenerator.generate("")).matches("\\d{20}");
    }

    /**
     * 验证 prefix 包含特殊字符时会自动过滤非法字符。
     */
    @Test
    void generate_specialPrefix_shouldFilterInvalidCharacters() {
        String orderNo = OrderNoGenerator.generate(" PA中文Y_#-01 ");

        assertThat(orderNo).matches("PAY_-01\\d{20}");
    }
}
