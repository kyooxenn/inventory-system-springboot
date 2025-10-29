package com.java.inventory.system.config;

import com.java.inventory.system.service.CustomUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
@Slf4j
public class TelegramBotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CustomUserDetailsService userService;

    @Bean
    public TelegramLongPollingBot telegramBot() {
        return new TelegramLongPollingBot() {
            @Override
            public void onUpdateReceived(Update update) {
                if (update.hasMessage() && update.getMessage().hasText()) {
                    String messageText = update.getMessage().getText();
                    long chatId = update.getMessage().getChatId();

                    log.info("Received message: {}", messageText);
                    log.info("Chat ID: {}", chatId);

                    if ((messageText.startsWith("/start ") || messageText.startsWith("/link ")) && messageText.split(" ").length > 1) {
                        String code = messageText.split(" ")[1];
                        log.info("Extracted code: {}", code);

                        String userId = redisTemplate.opsForValue().get(code);
                        log.info("Retrieved userId from Redis: {}", userId);

                        if (userId != null) {
                            saveChatIdToUser(userId, String.valueOf(chatId));
                            redisTemplate.delete(code);
                            log.info("Account linked for user: {}", userId);

                            SendMessage confirm = new SendMessage();
                            confirm.setChatId(String.valueOf(chatId));
                            confirm.setText("Your account is now linked! You can receive OTPs here.");
                            try {
                                execute(confirm);
                            } catch (TelegramApiException e) {
                                log.error("Error sending confirmation: {}", e.getMessage());
                            }
                        } else {
                            log.warn("Invalid code: {}", code);
                            SendMessage error = new SendMessage();
                            error.setChatId(String.valueOf(chatId));
                            error.setText("Invalid linking code. Please try again.");
                            try {
                                execute(error);
                            } catch (TelegramApiException e) {
                                log.error("Error sending error: {}", e.getMessage());
                            }
                        }
                    } else {
                        log.info("Message does not match linking format.");
                    }
                }
            }

            private void saveChatIdToUser(String userId, String chatId) {
                try {
                    userService.updateTelegramChatId(userId, chatId);
                    log.info("Chat ID saved for user: {}", userId);
                } catch (Exception e) {
                    log.error("Error saving Chat ID: {}", e.getMessage());
                }
            }

            @Override
            public String getBotUsername() {
                return "nvent_otp_generator_bot";
            }

            @Override
            public String getBotToken() {
                return botToken;
            }
        };
    }
}