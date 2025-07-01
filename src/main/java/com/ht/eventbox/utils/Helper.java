package com.ht.eventbox.utils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

public class Helper {
    public static String generateRandomSecret(int length) {
        byte[] randomBytes = new byte[length];
        new SecureRandom().nextBytes(randomBytes);

        // Use Base64 encoding to represent the random bytes as a string
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String generateOTP() {
        String digits = "0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(digits.length());
            otp.append(digits.charAt(index));
        }
        return otp.toString();
    }

    public static String formatDateToString(Date date) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(date);
    }

    public static String formatCurrencyToString(BigDecimal amount) {
        java.text.NumberFormat currencyFormatter = java.text.NumberFormat.getCurrencyInstance();
        return currencyFormatter.format(amount);
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }
}
