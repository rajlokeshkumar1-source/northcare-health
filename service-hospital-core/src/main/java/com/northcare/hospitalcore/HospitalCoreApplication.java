package com.northcare.hospitalcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class HospitalCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(HospitalCoreApplication.class, args);
    }
}
