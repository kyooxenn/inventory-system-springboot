package com.java.inventory.system.dto;

import lombok.Data;

@Data
public class AuthRequest {
    String username;
    String password;
    String email;
    String mobile;
}
