package com.java.inventory.system.config;

import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            // Check if root user already exists
            if (userRepository.findByUsername("root").isEmpty()) {
                User u = new User();
                u.setUsername("root");
                u.setPassword(passwordEncoder.encode("root"));
                u.setRoles("ROLE_ADMIN");
                u.setEmail("bobila.norbert@gmail.com");
                u.setMobile("+639603717056");
                userRepository.save(u);
            }
        };
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
