package com.ht.eventbox.utils;

import com.ht.eventbox.entities.Asset;
import com.ht.eventbox.enums.AssetUsage;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

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

    public static String formatDateToString(LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss");
        return date.format(formatter);
    }

    public static String formatCurrencyToString(Double amount) {
        java.text.NumberFormat currencyFormatter = java.text.NumberFormat.getCurrencyInstance();
        return currencyFormatter.format(amount);
    }

    public static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public static Asset getAssetFromUploadResult(Map uploadResult, AssetUsage usage) {
        return Asset.builder()
                .resourceType(String.valueOf(uploadResult.get("resource_type")))
                .publicId(String.valueOf(uploadResult.get("public_id")))
                .signature(String.valueOf(uploadResult.get("signature")))
                .id(String.valueOf(uploadResult.get("asset_id")))
                .originalUrl(String.valueOf(uploadResult.get("url")))
                .secureUrl(String.valueOf(uploadResult.get("secure_url")))
                .folder(String.valueOf(uploadResult.get("folder")))
                .format(String.valueOf(uploadResult.get("format")))
                .width(Integer.parseInt(String.valueOf(uploadResult.get("width"))))
                .height(Integer.parseInt(String.valueOf(uploadResult.get("height"))))
                .bytes(Long.parseLong(String.valueOf(uploadResult.get("bytes"))))
                .eTag(String.valueOf(uploadResult.get("etag")))
                .usage(usage)
                .build();
    }
}
