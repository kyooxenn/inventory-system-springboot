package com.java.inventory.system.config;


import com.java.inventory.system.security.JwtAuthenticationFilter;
import com.java.inventory.system.security.JwtUtil;
import com.java.inventory.system.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final CustomUserDetailsService userDetailsService;
  private final JwtUtil jwtUtil;
  private final BCryptPasswordEncoder passwordEncoder;

  public SecurityConfig(CustomUserDetailsService userDetailsService,
                        JwtUtil jwtUtil,
                        BCryptPasswordEncoder passwordEncoder) {
    this.userDetailsService = userDetailsService;
    this.jwtUtil = jwtUtil;
    this.passwordEncoder = passwordEncoder;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/api/auth/**").permitAll()
          .anyRequest().authenticated()
      )
      .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }
}
