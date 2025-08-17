package com.emod.emod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.emod")
public class EmodApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmodApplication.class, args);
    }

}
