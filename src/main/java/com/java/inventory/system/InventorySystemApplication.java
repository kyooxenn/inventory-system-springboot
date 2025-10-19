package com.java.inventory.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableCaching
// 👇 This disables Redis repository scanning but keeps Redis available for caching.
@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableScheduling // keep application alive on render, prevents shutdown due to inactivity
public class InventorySystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventorySystemApplication.class, args);
	}

}
