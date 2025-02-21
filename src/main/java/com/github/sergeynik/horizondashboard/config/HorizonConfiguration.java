package com.github.sergeynik.horizondashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;

@Configuration
public class HorizonConfiguration {

    @Value("${horizon.url}")
    private String url;

    @Bean
    public Server horizonServer() {
        return new Server(url);
    }
}
