package com.dinepilot.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.web.client.RestClient;

@SpringBootApplication(scanBasePackages = "com.dinepilot")
@EnableMongoAuditing
public class ReservationServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ReservationServiceApplication.class, args); }

    @Bean
    @LoadBalanced
    RestClient.Builder restClientBuilder() { return RestClient.builder(); }
}
