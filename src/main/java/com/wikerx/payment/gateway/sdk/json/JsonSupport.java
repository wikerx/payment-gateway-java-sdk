package com.wikerx.payment.gateway.sdk.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wikerx.payment.gateway.sdk.exception.PaymentGatewayResponseException;

import java.util.List;

/**
 * SDK JSON 序列化工具，统一控制字段兼容和未知字段容忍策略。
 */
public final class JsonSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
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
            throw new PaymentGatewayResponseException("OpenAPI request json serialization failed", exception);
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
            throw new PaymentGatewayResponseException("OpenAPI response json parse failed", exception);
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
            throw new PaymentGatewayResponseException("OpenAPI response json parse failed", exception);
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
            throw new PaymentGatewayResponseException("OpenAPI response json list parse failed", exception);
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
