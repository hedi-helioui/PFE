package com.pfe.ekyc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EkycApplication {

    public static void main(String[] args) {
        SpringApplication.run(EkycApplication.class, args);
    }
}
