package com.mediquery.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.mediquery.agent")
public class MediQueryAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediQueryAgentApplication.class, args);
    }
}
