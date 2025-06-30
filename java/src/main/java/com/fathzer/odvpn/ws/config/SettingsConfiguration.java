package com.fathzer.odvpn.ws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fathzer.odvpn.repository.VPNRepositorySettings;

@Configuration
public class SettingsConfiguration {

    @Bean
    public VPNRepositorySettings validatedSettings() {
        return VPNRepositorySettings.fromEnvironment(true);
    }
}
