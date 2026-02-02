package com.sba.ssos.utils;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
public final  class OrderCodeUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private OrderCodeUtils() {}

    public static String generate(String prefix) {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        String randomPart = randomAlphaNumeric(6); // 6 ký tự

        return prefix + "-" + datePart + "-" + randomPart;
    }

    private static String randomAlphaNumeric(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        // bỏ I, O, 0, 1 để tránh nhầm

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
