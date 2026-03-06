package com.telecom.platform.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.telecom.platform.support", "com.telecom.platform.common"})
public class SupportSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportSearchApplication.class, args);
    }
}
