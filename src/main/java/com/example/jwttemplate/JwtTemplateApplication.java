package com.example.jwttemplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class JwtTemplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(JwtTemplateApplication.class, args);
    }
}
