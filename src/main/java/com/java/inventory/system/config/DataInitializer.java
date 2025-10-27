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
      if (userRepository.findByUsername("user").isEmpty()) {
        User u = new User();
        u.setUsername("user");
        u.setPassword(passwordEncoder.encode("password"));
        u.setRoles("ROLE_USER");
        u.setEmail("email");
        u.setMobile("mobile");
        userRepository.save(u);
      }
    };
  }

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
