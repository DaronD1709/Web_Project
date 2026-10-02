package com.ecommerce.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Bam mat khau bang PBKDF2 co san trong JDK (khong can them thu vien).
 * Dinh dang luu trong cot password_hash:  "so_vong_lap:salt:hash"  (salt/hash ma hoa Base64).
 * Moi mat khau co salt ngau nhien rieng -> 2 nguoi cung mat khau van ra 2 hash khac nhau.
 * (Luu y: "javax.crypto" la goi cua JDK, KHONG phai javax.persistence/servlet nen khong bi doi sang jakarta.)
 */
public class PasswordUtil {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;

    public static String hash(String rawPassword) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String rawPassword, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);
        byte[] actual = pbkdf2(rawPassword, salt, Integer.parseInt(parts[0]));
        return MessageDigest.isEqual(expected, actual); // so sanh hang thoi gian, tranh timing attack
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Khong bam duoc mat khau", e);
        }
    }
}
