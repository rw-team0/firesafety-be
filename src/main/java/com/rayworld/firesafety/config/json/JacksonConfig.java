package com.rayworld.firesafety.config.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // JSON 변환기 빈 등록
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
