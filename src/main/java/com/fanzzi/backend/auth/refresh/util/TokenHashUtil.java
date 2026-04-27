package com.fanzzi.backend.auth.refresh.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class TokenHashUtil {

    private TokenHashUtil() {}

    public static String sha256(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(token.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                String part = Integer.toHexString(0xff & b);
                if (part.length() == 1) hex.append('0');
                hex.append(part);
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public static boolean secureEquals(String a, String b) {
        if (a == null || b == null) return false;

        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}