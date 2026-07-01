package com.scott.payment.sdk.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scott.payment.sdk.exception.OpenApiResponseException;

import java.util.List;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : JsonSupport
 * @date : 2026-07-01 11:08
 * @email : scott_x@163.com
 * @description : SDK JSON 序列化工具，负责统一控制请求序列化、响应反序列化、列表解析和未知字段容忍策略。
 *                本类不执行签名、加密、HTTP 调用或资金状态处理；序列化对象可能包含敏感业务字段，调用方输出日志前必须先脱敏。
 * @status : modify
 */
public final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * 日志专用 JSON 序列化器。
     *
     * 该对象只用于日志输出，排除 null 字段，避免商户联调时被无效字段干扰；真实 HTTP 请求体仍使用 OBJECT_MAPPER。
     */
    private static final ObjectMapper LOG_OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonSupport() {
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     */
    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new OpenApiResponseException("OpenAPI request json serialization failed", exception);
        }
    }

    /**
     * 将对象序列化为日志 JSON 字符串。
     *
     * 日志 JSON 会排除 null 字段，仅用于商户联调核验和 SDK 本地日志，不影响真实请求报文。
     *
     * @param value 待序列化对象
     * @return 排除 null 字段后的 JSON 字符串
     */
    public static String toLogJson(Object value) {
        try {
            return LOG_OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new OpenApiResponseException("OpenAPI log json serialization failed", exception);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型。
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 反序列化结果
     */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new OpenApiResponseException("OpenAPI response json parse failed", exception);
        }
    }

    /**
     * 将 JSON 字符串反序列化为泛型类型。
     *
     * @param json          JSON 字符串
     * @param typeReference 目标泛型类型
     * @param <T>           目标类型
     * @return 反序列化结果
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            throw new OpenApiResponseException("OpenAPI response json parse failed", exception);
        }
    }

    /**
     * 将 JSON 字符串反序列化为列表。
     *
     * @param json        JSON 字符串
     * @param elementType 列表元素类型
     * @param <T>         列表元素类型
     * @return 列表结果
     */
    public static <T> List<T> fromJsonList(String json, Class<T> elementType) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException exception) {
            throw new OpenApiResponseException("OpenAPI response json list parse failed", exception);
        }
    }

    /**
     * 返回共享 ObjectMapper，主要用于测试和扩展场景。
     *
     * @return 共享 ObjectMapper
     */
    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }
}
