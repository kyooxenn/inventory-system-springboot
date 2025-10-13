package com.java.inventory.system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

  private final Key key;
  private final long expirationMs;

  public JwtUtil(@Value("${jwt.secret}") String secret,
                 @Value("${jwt.expiration-ms}") long expirationMs) {
    // secret should be base64 or raw bytes length >= 256 bits for HS256
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
    this.expirationMs = expirationMs;
  }

  public String generateToken(String username, String roles) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .setSubject(username)
        .claim("roles", roles)
        .setIssuedAt(now)
        .setExpiration(expiry)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (JwtException | IllegalArgumentException ex) {
      // log if needed
      return false;
    }
  }

  public String getUsername(String token) {
    Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
        .parseClaimsJws(token).getBody();
    return claims.getSubject();
  }

  public String getRoles(String token) {
    Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
        .parseClaimsJws(token).getBody();
    return claims.get("roles", String.class);
  }
}
