package com.ecommerce.service;

import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.User;
import com.ecommerce.util.PasswordUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Dang ky, dang nhap, quen/dat lai mat khau. Moi rule validate nam o day, khong nam o Servlet. */
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int RESET_TOKEN_MINUTES = 30;

    private final UserDAO userDAO = new UserDAO();
    private final EmailService emailService = new EmailService();

    /** Dang ky khach hang moi (kem gio hang rong) va gui email chao mung. */
    public Customer register(String fullName, String email, String phone, String password, String confirm) {
        Map<String, String> errors = new LinkedHashMap<>();
        email = normalizeEmail(email);

        if (fullName == null || fullName.isBlank()) errors.put("fullName", "Vui lòng nhập họ tên");
        if (!EMAIL.matcher(email).matches()) errors.put("email", "Email không hợp lệ");
        else if (userDAO.findByEmail(email) != null) errors.put("email", "Email này đã được đăng ký");
        validatePassword(password, confirm, "password", "confirmPassword", errors);
        if (!errors.isEmpty()) throw new BusinessException(errors);

        Customer customer = new Customer();
        customer.setFullName(fullName.trim());
        customer.setEmail(email);
        customer.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        customer.setPasswordHash(PasswordUtil.hash(password));
        customer.setCreatedAt(LocalDateTime.now());
        Cart cart = new Cart();           // Customer 1-1 Cart: tao san gio rong
        cart.setCustomer(customer);
        customer.setCart(cart);
        userDAO.save(customer);

        emailService.sendWelcome(customer); // chay nen, loi gui mail khong lam hong dang ky
        return customer;
    }

    /** Sai email hay sai mat khau deu bao CUNG 1 loi, de ke xau khong do duoc email nao da dang ky. */
    public User login(String email, String password) {
        User user = userDAO.findByEmail(normalizeEmail(email));
        if (user == null || password == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            throw new BusinessException("Email hoặc mật khẩu không đúng.");
        }
        return user;
    }

    /**
     * Quen mat khau. Email co ton tai hay khong deu KHONG bao khac nhau cho nguoi goi (chong do email);
     * chi khi co that moi tao token + gui link. baseUrl vd: http://localhost:8080/ecommerce
     */
    public void requestPasswordReset(String email, String baseUrl) {
        User user = userDAO.findByEmail(normalizeEmail(email));
        if (user == null) return;

        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw); // token goc: chi nam trong email
        user.setResetTokenHash(sha256Hex(token));                                   // DB chi luu ban bam
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES));
        userDAO.update(user);

        emailService.sendPasswordReset(user, baseUrl + "/reset-password?token=" + token);
    }

    public boolean isResetTokenValid(String token) {
        return findUserByValidToken(token) != null;
    }

    /** Dat mat khau moi bang token trong email. Token dung 1 lan: dung xong xoa luon. */
    public void resetPassword(String token, String newPassword, String confirm) {
        Map<String, String> errors = new LinkedHashMap<>();
        validatePassword(newPassword, confirm, "password", "confirmPassword", errors);
        if (!errors.isEmpty()) throw new BusinessException(errors);

        User user = findUserByValidToken(token);
        if (user == null) throw new BusinessException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");

        user.setPasswordHash(PasswordUtil.hash(newPassword));
        user.setResetTokenHash(null);
        user.setResetTokenExpiry(null);
        userDAO.update(user);
    }

    // ------------------------------------------------------------------ noi bo

    private User findUserByValidToken(String token) {
        if (token == null || token.isBlank()) return null;
        User user = userDAO.findByResetTokenHash(sha256Hex(token));
        if (user == null || user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return null;
        }
        return user;
    }

    private static void validatePassword(String password, String confirm, String passwordField,
                                         String confirmField, Map<String, String> errors) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            errors.put(passwordField, "Mật khẩu tối thiểu " + MIN_PASSWORD_LENGTH + " ký tự");
        } else if (!password.equals(confirm)) {
            errors.put(confirmField, "Mật khẩu nhập lại không khớp");
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String sha256Hex(String s) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
