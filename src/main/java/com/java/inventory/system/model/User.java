package com.java.inventory.system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor // ✅ fixes the error
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String mobile;

    @Builder.Default // Ensures default value in builder
    @Column(nullable = false)
    private Boolean isVerified = false;

    // store roles as comma separated or a join table; simple example:
    private String roles; // e.g. "ROLE_USER,ROLE_ADMIN"

    private String telegramChatId;

}
