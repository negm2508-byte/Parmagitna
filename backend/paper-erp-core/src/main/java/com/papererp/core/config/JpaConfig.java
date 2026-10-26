package com.papererp.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.papererp.core.domain")
@EnableJpaRepositories(basePackages = "com.papererp.core.repository")
public class JpaConfig {
}
