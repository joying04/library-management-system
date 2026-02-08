package com.wxx.library.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Configuration
public class JacksonConfig {

    // 全局统一日期格式化规则（适配JWT中时间字段、业务中的时间参数等）
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public ObjectMapper objectMapper() {
        // 用JsonMapper.builder()构建
        ObjectMapper objectMapper = JsonMapper.builder()
                // 注册Java8时间模块（处理LocalDateTime，避免响应时序列化失败）
                .addModule(javaTimeModule())
                // 注册Long转String模块（避免前端Long精度丢失，比如用户ID、图书ID）
                .addModule(longToStringModule())
                .build();

        // 忽略Token/请求参数中的未知字段（提高兼容性，避免解析时因字段不匹配报错）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 关闭空对象序列化异常（拦截器返回空Result时不报错）
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 序列化时忽略null字段（让响应JSON更简洁）
        objectMapper.setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

        return objectMapper;
    }

    /**
     * 配置Java8时间（LocalDateTime）的序列化/反序列化规则
     */
    private JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        // 序列化：LocalDateTime → "yyyy-MM-dd HH:mm:ss"字符串（如借阅时间返回给前端）
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(LOCAL_DATE_TIME_FORMATTER));
        // 反序列化：前端传的"yyyy-MM-dd HH:mm:ss"字符串 → LocalDateTime（如查询时间参数）
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(LOCAL_DATE_TIME_FORMATTER));
        return module;
    }

    /**
     * Long转String，包含数组、集合等场景
     */
    private SimpleModule longToStringModule() {
        SimpleModule module = new SimpleModule();
        // 1. 基础类型：Long包装类、long基本类型
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(long.class, ToStringSerializer.instance);
        // 2. 数组类型：Long[]、long[]（前端接收数组时避免精度丢失）
        module.addSerializer(Long[].class, ToStringSerializer.instance);
        module.addSerializer(long[].class, ToStringSerializer.instance);
        // 3. 集合类型：List<Long>、Set<Long>（适配集合序列化场景）
        module.addSerializer(List.class, new ToStringSerializer(Long.class));
        module.addSerializer(Set.class, new ToStringSerializer(Long.class));
        return module;
    }
}
