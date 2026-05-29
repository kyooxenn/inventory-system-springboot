package com.java.inventory.system.dto;

import lombok.Data;

@Data
public class AuthenticationRequest {
    String username;
    String password;
    String email;
    String mobile;
}
