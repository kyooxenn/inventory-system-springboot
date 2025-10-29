package com.java.inventory.system.util;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

@UtilityClass
public class OtpGenerator {
    private static final SecureRandom random = new SecureRandom();

    public static String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {  // Fixed to 6 digits
            otp.append(random.nextInt(10));  // 0-9 digits
        }
        return otp.toString();
    }
}