package com.java.inventory.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableCaching
@EnableJpaRepositories(basePackages = "com.java.inventory.system.repository")
@EntityScan(basePackages = "com.java.inventory.system.model")
// 👇 disables redis repository scanning since you only use it for caching
@EnableRedisRepositories(basePackages = "none")
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
