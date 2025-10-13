package com.java.inventory.system.controller;

import com.java.inventory.system.model.User;
import com.java.inventory.system.repository.UserRepository;
import com.java.inventory.system.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

record AuthRequest(String username, String password) {}
record AuthResponse(String token) {}

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  public AuthController(AuthenticationManager authenticationManager,
                        JwtUtil jwtUtil,
                        UserRepository userRepository,
                        BCryptPasswordEncoder passwordEncoder) {
    this.authenticationManager = authenticationManager;
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody @Valid AuthRequest request) {
    try {
      Authentication auth = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.username(), request.password())
      );
      // load user to read roles
      var user = userRepository.findByUsername(request.username()).orElseThrow();
      String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());
      return ResponseEntity.ok(new AuthResponse(token));
    } catch (BadCredentialsException ex) {
      return ResponseEntity.status(401).body("Invalid credentials");
    }
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody @Valid AuthRequest request) {
    if (userRepository.findByUsername(request.username()).isPresent()) {
      return ResponseEntity.badRequest().body("Username already exists");
    }
    User u = new User();
    u.setUsername(request.username());
    u.setPassword(passwordEncoder.encode(request.password()));
    u.setRoles("ROLE_USER");
    userRepository.save(u);
    return ResponseEntity.ok("User registered");
  }
}
