package com.scott.payment.sdk.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OrderNoGenerator
 * @date : 2026-07-01 16:42
 * @email : scott_x@163.com
 * @description : SDK 本地订单号生成工具，负责为商户示例、测试用例和本地联调快速生成 merchantOrderNo。
 *                本类只保证单 JVM 进程内同一毫秒高并发生成不重复，不提供分布式全局唯一能力，不依赖数据库、Redis、Spring 或第三方工具库。
 *                生成结果不涉及资金计算、状态流转、签名、加密或外部渠道调用；商户生产系统如有全局订单号规则，应优先使用自身订单号体系。
 * @status : create
 */
public final class OrderNoGenerator {

    /**
     * 订单号时间部分格式，精确到毫秒。
     *
     * 线程安全：SimpleDateFormat 非线程安全，因此通过 ThreadLocal 为每个线程提供独立实例。
     */
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyyMMddHHmmssSSS");
                }
            };

    /**
     * 同一毫秒内最大序号值。
     */
    private static final int MAX_SEQUENCE = 999;

    /**
     * 最近一次生成订单号使用的毫秒时间戳。
     */
    private static long lastTimestamp = -1L;

    /**
     * 同一毫秒内递增序号，范围 000-999。
     */
    private static int sequence = 0;

    private OrderNoGenerator() {
    }

    /**
     * 生成不带前缀的订单号。
     *
     * 该方法只在当前 JVM 进程内保证不重复，不访问数据库、Redis 或外部服务，不修改支付、代付、退款或资金状态。
     * 生成结果格式为 yyyyMMddHHmmssSSS + 三位序号，适合 SDK 示例和商户本地联调使用。
     *
     * @return 不带前缀的本地订单号
     */
    public static String generate() {
        return generate(null);
    }

    /**
     * 生成带前缀的订单号。
     *
     * prefix 会自动过滤非字母、数字、下划线、中划线的字符，避免商户误传空格、中文或特殊符号导致网关订单号格式不可控。
     * 同一毫秒内最多生成 1000 个订单号；超过后会等待进入下一毫秒再继续生成，确保当前 JVM 进程内不重复。
     * 本方法不参与签名、加密、对账、清分或结算，不保证分布式多进程全局唯一。
     *
     * @param prefix 订单号前缀，例如 PAY、ORDER、TEST；允许为空，非法字符会自动移除
     * @return prefix + yyyyMMddHHmmssSSS + 三位序号格式的本地订单号
     */
    public static synchronized String generate(String prefix) {
        long current = System.currentTimeMillis();
        if (current < lastTimestamp) {
            current = lastTimestamp;
        }

        if (current == lastTimestamp) {
            sequence++;
            if (sequence > MAX_SEQUENCE) {
                current = waitNextMillis(lastTimestamp);
                sequence = 0;
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = current;
        return cleanPrefix(prefix) + DATE_FORMAT.get().format(new Date(current)) + formatSequence(sequence);
    }

    /**
     * 等待进入下一毫秒。
     *
     * 该方法只在单 JVM 内序号耗尽时短暂自旋，不依赖外部时钟服务或分布式锁。
     *
     * @param timestamp 已经使用过的毫秒时间戳
     * @return 大于 timestamp 的当前毫秒时间戳
     */
    private static long waitNextMillis(long timestamp) {
        long current = System.currentTimeMillis();
        while (current <= timestamp) {
            Thread.yield();
            current = System.currentTimeMillis();
        }
        return current;
    }

    /**
     * 清理订单号前缀。
     *
     * @param prefix 商户传入的前缀
     * @return 只保留 ASCII 字母、数字、下划线和中划线的前缀
     */
    private static String cleanPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(prefix.length());
        for (int index = 0; index < prefix.length(); index++) {
            char value = prefix.charAt(index);
            if (isAllowedPrefixChar(value)) {
                builder.append(value);
            }
        }
        return builder.toString();
    }

    /**
     * 判断前缀字符是否允许进入订单号。
     *
     * @param value 待校验字符
     * @return true 表示允许，false 表示过滤
     */
    private static boolean isAllowedPrefixChar(char value) {
        return value >= 'A' && value <= 'Z'
                || value >= 'a' && value <= 'z'
                || value >= '0' && value <= '9'
                || value == '_'
                || value == '-';
    }

    /**
     * 格式化同毫秒序号。
     *
     * @param value 序号值，范围 0-999
     * @return 三位序号文本
     */
    private static String formatSequence(int value) {
        if (value < 10) {
            return "00" + value;
        }
        if (value < 100) {
            return "0" + value;
        }
        return String.valueOf(value);
    }
}
