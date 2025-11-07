package com.java.inventory.system.config;

import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    // create admin user when application started.
    @Bean
    CommandLineRunner init(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            // Check if admin user already exists
            if (userRepository.findByUsername("admin").isEmpty()) {
                User u = new User();
                u.setUsername("admin");
                u.setPassword(passwordEncoder.encode("1234"));
                u.setRoles("ROLE_ADMIN");
                u.setEmail("norbertbobila12@gmail.com");
                u.setMobile("+639603717056");
                u.setIsVerified(false);
                userRepository.save(u);
            }
        };
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
