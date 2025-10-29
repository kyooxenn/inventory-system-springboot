package com.java.inventory.system;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@EnableCaching
// 👇 This disables Redis repository scanning but keeps Redis available for caching.
@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableScheduling // keep application alive on render, prevents shutdown due to inactivity
@Slf4j
public class InventorySystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventorySystemApplication.class, args);
	}

    @Component
    public static class BotInitializer implements CommandLineRunner {
        @Autowired
        private TelegramLongPollingBot telegramBot;
        @Override
        public void run(String... args) throws Exception {
            try {
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                botsApi.registerBot(telegramBot);
                log.info("Telegram bot registered and polling started!");
            } catch (TelegramApiException e) {
                log.error("Failed to register bot: " + e.getMessage());
            }
        }
    }

}
