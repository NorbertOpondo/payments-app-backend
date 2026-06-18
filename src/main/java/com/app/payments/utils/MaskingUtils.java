package com.app.payments.utils;

import java.util.concurrent.ThreadLocalRandom;

public final class MaskingUtils {

    private MaskingUtils() {}

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 3) return "***";
        return phone.substring(0, 3) + "*".repeat(phone.length() - 3);
    }

    public static String generateReceiptNumber(String prefix) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 7; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }
}
