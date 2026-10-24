package com.papererp.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.papererp")
@EntityScan(basePackages = "com.papererp")
@EnableJpaRepositories(basePackages = "com.papererp")
public class PaperErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaperErpApplication.class, args);
    }
}
