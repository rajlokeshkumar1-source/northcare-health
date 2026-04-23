package com.northcare.telehealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TelehealthApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelehealthApplication.class, args);
    }
}
