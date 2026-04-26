package com.rag.backend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 配置类
 * 支持 Java 8 日期时间类型的序列化和反序列化
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 JavaTimeModule 以支持 JDK 8 日期/时间类型
        mapper.registerModule(new JavaTimeModule());
        // 禁用将日期写入为时间戳的格式，使用 ISO-8601 格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
