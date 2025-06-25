package com.fathzer.terravpn.ws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.Module;
import com.fathzer.terravpn.json.CustomSerializationModule;

@Configuration
public class JacksonConfig {

    @Bean
    public Module customSerializationModule() {
        return new CustomSerializationModule();
    }

    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .modulesToInstall(customSerializationModule());
    }
}
