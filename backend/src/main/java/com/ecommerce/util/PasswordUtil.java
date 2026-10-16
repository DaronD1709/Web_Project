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
 *
 * Tai sao khong luu mat khau truc tiep / khong dung SHA-256 don gian?
 *  - Ham bam la MOT CHIEU: tu hash khong suy nguoc ra mat khau. DB bi lo cung khong doc duoc mat khau that.
 *  - SHA-256 don gian chay rat nhanh -> ke tan cong thu hang ty mat khau moi giay. PBKDF2 lap lai ham bam hang
 *    tram nghin vong (ITERATIONS) nen moi lan thu ton thoi gian: nguoi dung dang nhap van nhanh (vai chuc ms),
 *    ke do mat khau thi cham di hang tram nghin lan.
 *  - Salt ngau nhien rieng moi mat khau: chan bang tra san (rainbow table) va chan viec do 1 lan cho ca bang users.
 *
 * Vi du gia tri luu:  120000:q2Vx...(salt 16 byte base64):9fK...(hash 32 byte base64)
 *
 * Duoc goi tu: AuthService (dang ky, dang nhap, dat lai mat khau), UserService (doi mat khau), ChatService/DataSeeder
 * (tao mat khau ngau nhien khong ai dung duoc cho tai khoan khach vang lai va bot).
 */
public class PasswordUtil {

    // So vong lap PBKDF2. Cang cao cang cham/an toan; duoc luu KEM trong chuoi hash nen sau nay tang so nay
    // thi mat khau cu van kiem tra duoc (verify doc so vong tu chinh chuoi da luu), chi hash moi dung so moi.
    // (Khuyen nghi hien nay cho PBKDF2-HMAC-SHA256 la nhieu hon, ~600.000; 120.000 la muc can bang cho may demo do an.)
    private static final int ITERATIONS = 120_000;
    // Do dai khoa dau ra: 256 bit = 32 byte, khop voi do dai cua SHA-256.
    private static final int KEY_BITS = 256;

    /**
     * Bam mat khau de LUU vao DB. Moi lan goi sinh salt moi nen cung 1 mat khau ra ket qua khac nhau (binh thuong);
     * kiem tra mat khau phai dung verify(), KHONG so sanh chuoi hash(a).equals(hash(a)).
     */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt); // SecureRandom = nguon ngau nhien du manh cho bao mat (khac java.util.Random)
        byte[] hash = pbkdf2(rawPassword, salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Kiem tra mat khau nguoi dung nhap co khop chuoi da luu trong DB khong.
     * Tach chuoi luu thanh 3 phan, bam lai mat khau nhap voi DUNG salt va so vong cua chuoi do, roi so sanh hai hash.
     * Tra false (khong nem loi) neu chuoi luu sai dinh dang; chuoi luu null thi se nem NullPointerException (DB khong nen co null).
     */
    public static boolean verify(String rawPassword, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        byte[] salt = Base64.getDecoder().decode(parts[1]);
        byte[] expected = Base64.getDecoder().decode(parts[2]);
        byte[] actual = pbkdf2(rawPassword, salt, Integer.parseInt(parts[0]));
        // MessageDigest.isEqual so sanh 2 mang byte trong thoi gian KHONG phu thuoc vao vi tri byte dau tien khac nhau.
        // equals/Arrays.equals dung som khi gap byte sai -> ke tan cong do thoi gian tra loi de doan dan tung byte (timing attack).
        return MessageDigest.isEqual(expected, actual); // so sanh hang thoi gian, tranh timing attack
    }

    /** Tinh PBKDF2 (HMAC-SHA256): mat khau + salt + so vong -> 32 byte. Tach rieng de hash() va verify() dung chung. */
    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            // PBEKeySpec nhan mat khau dang char[] (mang ky tu) chu khong phai String
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            // Chi xay ra neu JDK thieu thuat toan nay (khong xay ra voi JDK 17), nen coi la loi cau hinh may chu, khong phai loi nguoi dung
            throw new IllegalStateException("Khong bam duoc mat khau", e);
        }
    }
}
