package com.java.inventory.system.config;

import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
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

import java.util.Objects;

@Configuration
@Slf4j
public class TelegramBotConfig {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CustomUserDetailsService userService;

    @Autowired
    private UserRepository userRepository;

    @Bean
    public TelegramLongPollingBot telegramBot() {
        return new TelegramLongPollingBot() {
            @Override
            public void onUpdateReceived(Update update) {
                if (update.hasMessage() && update.getMessage().hasText()) {
                    String messageText = update.getMessage().getText();
                    String chatId = update.getMessage().getChatId().toString();

                    log.info("Received message: {}", messageText);
                    log.info("Chat ID: {}", chatId);

                    if ((messageText.startsWith("/start ") || messageText.startsWith("/link ")) && messageText.split(" ").length > 1) {
                        String code = messageText.split(" ")[1];
                        log.info("Extracted code: {}", code);

                        String username = redisTemplate.opsForValue().get(code);
                        log.info("Retrieved username from Redis: {}", username);

                        if (username == null) {
                            sendMessage(chatId, "Invalid or expired linking code. Please try again.");
                            return;
                        }

                        User user = userRepository.findByUsername(username).orElse(null);
                        if (user == null) {
                            sendMessage(chatId, "User not found. Please try again.");
                            return;
                        }

                        // ✅ Check if this chat ID is already linked to another user
                        User existing = userRepository.findByTelegramChatId(chatId).orElse(null);
                        if (existing != null && !Objects.equals(existing.getUsername(), username)) {
                            log.warn("Chat ID {} already linked to another account ({})", chatId, existing.getUsername());
                            sendMessage(chatId, "⚠️ This Telegram account is already linked to another user. You cannot link it to a different account.");

                            // 🔥 Store failure result in Redis for frontend to read
                            redisTemplate.opsForValue().set("linkResult:" + code, "already_linked");
                            return;
                        }


                        // ✅ If same user is re-linking, just confirm
                        if (Objects.equals(chatId, user.getTelegramChatId())) {
                            redisTemplate.delete(code);
                            redisTemplate.opsForValue().set("linkResult:" + code, "success");
                            sendMessage(chatId, "✅ Your account is already linked! You can receive OTPs here.");
                            return;
                        }

                        // ✅ Otherwise, save new link
                        saveChatIdToUser(username, chatId);
                        redisTemplate.delete(code);
                        redisTemplate.opsForValue().set("linkResult:" + code, "success");
                        sendMessage(chatId, "✅ Your account has been successfully linked! You can now receive OTPs here.");

                    } else {
                        log.info("Message does not match linking format.");
                    }
                }
            }

            private void saveChatIdToUser(String username, String chatId) {
                try {
                    userService.updateTelegramChatId(username, chatId);
                    log.info("Chat ID saved for user: {}", username);
                } catch (Exception e) {
                    log.error("Error saving Chat ID: {}", e.getMessage());
                }
            }

            private void sendMessage(String chatId, String text) {
                try {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText(text);
                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("Error sending message: {}", e.getMessage());
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
