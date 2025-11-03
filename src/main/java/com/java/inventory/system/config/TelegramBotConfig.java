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
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
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
                if (!update.hasMessage()) return;

                String chatId = update.getMessage().getChatId().toString();

                // ✅ 1. Handle contact sharing (after pressing "Share my number")
                if (update.getMessage().hasContact()) {
                    handleContactVerification(update, chatId);
                    return;
                }

                // ✅ 2. Handle /start or /link commands
                if (update.getMessage().hasText()) {
                    handleLinkCommand(update, chatId);
                }
            }

            /**
             * Handles the /start or /link command to begin Telegram account linking.
             */
            private void handleLinkCommand(Update update, String chatId) {
                String messageText = update.getMessage().getText();
                log.info("Received message: {}", messageText);

                if ((messageText.startsWith("/start ") || messageText.startsWith("/link "))
                        && messageText.split(" ").length > 1) {

                    String code = messageText.split(" ")[1];
                    log.info("Extracted code: {}", code);

                    String username = redisTemplate.opsForValue().get(code);
                    log.info("Retrieved username from Redis: {}", username);

                    if (username == null) {
                        sendMessage(chatId, "❌ Invalid or expired linking code. Please try again.");
                        return;
                    }

                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user == null) {
                        sendMessage(chatId, "❌ User not found. Please try again.");
                        return;
                    }

                    // ✅ Check if chat ID is already linked to another user
                    User existing = userRepository.findByTelegramChatId(chatId).orElse(null);
                    if (existing != null && !Objects.equals(existing.getUsername(), username)) {
                        log.warn("Chat ID {} already linked to another account ({})", chatId, existing.getUsername());
                        sendMessage(chatId, "⚠️ This Telegram account is already linked to another user.");
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

                    // ✅ Store pending user for later contact verification
                    redisTemplate.opsForValue().set("pendingUser:" + chatId, username);
                    redisTemplate.opsForValue().set("pendingCode:" + chatId, code);

                    sendMessage(chatId, "📱 Please share your phone number to verify your account:");
                    shareTelegramNumber(chatId);

                } else {
                    sendMessage(chatId, "Message does not match linking format.");
                    log.info("Message does not match linking format.");
                }
            }

            /**
             * Handles verification when user shares their contact number.
             */
            private void handleContactVerification(Update update, String chatId) {
                Contact contact = update.getMessage().getContact();
                String phoneNumber = contact.getPhoneNumber().replaceAll("[^0-9]", "");
                log.info("Received contact number: {}", phoneNumber);

                String username = redisTemplate.opsForValue().get("pendingUser:" + chatId);
                String code = redisTemplate.opsForValue().get("pendingCode:" + chatId);

                if (username == null) {
                    sendMessage(chatId, "⚠️ No pending verification found. Please use /link again.");
                    return;
                }

                User user = userRepository.findByUsername(username).orElse(null);
                if (user == null) {
                    sendMessage(chatId, "❌ User not found.");
                    return;
                }

                String dbPhone = user.getMobile().replaceAll("[^0-9]", "");

                // ✅ Verify phone number
                if (phoneNumber.equals(dbPhone)) {
                    sendMessage(chatId, "✅ Phone number verified successfully!");
                    saveChatIdToUser(username, chatId);
                    redisTemplate.opsForValue().set("linkResult:" + code, "success");
                    redisTemplate.delete("pendingUser:" + chatId);
                    redisTemplate.delete("pendingCode:" + chatId);
                    sendMessage(chatId, "🎉 Your account has been successfully linked! You can now receive OTPs here.");
                } else {
                    sendMessage(chatId, """
                ❌ The phone number you entered doesn’t match your Telegram-registered number.
                
                📱 Please make sure you used the same number that’s registered in your Telegram account.
                
                This ensures you can receive OTPs and verify your account successfully.
                """);
                }
            }

            private void saveChatIdToUser(String username, String chatId) {
                try {
                    userService.updateTelegramChatId(username, chatId);
                    log.info("✅ Chat ID saved for user: {}", username);
                } catch (Exception e) {
                    log.error("❌ Error saving Chat ID: {}", e.getMessage());
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

            private void shareTelegramNumber(String chatId) {
                try {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId);
                    message.setText("📞 Tap below to share your Telegram number:");

                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    keyboardMarkup.setResizeKeyboard(true);
                    keyboardMarkup.setOneTimeKeyboard(true);

                    KeyboardButton shareButton = new KeyboardButton("📲 Share my number");
                    shareButton.setRequestContact(true);

                    KeyboardRow row = new KeyboardRow();
                    row.add(shareButton);

                    keyboardMarkup.setKeyboard(List.of(row));
                    message.setReplyMarkup(keyboardMarkup);

                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("Error sending contact request: {}", e.getMessage());
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
