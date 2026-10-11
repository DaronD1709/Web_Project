package com.ecommerce.util;

import com.ecommerce.entity.Customer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Token riêng của giỏ hàng; không thay token Profile/Checkout hay luồng Admin. */
public final class CartCsrf {
    private static final String TOKEN = "cartCsrfToken";

    private CartCsrf() { }

    /** Các trang có form thêm hàng gọi hàm này trước khi forward JSP. */
    public static String prepare(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || !(SessionUtil.currentUser(req) instanceof Customer)) return null;
        String token;
        synchronized (session) {
            token = (String) session.getAttribute(TOKEN);
            if (token == null) {
                byte[] bytes = new byte[32];
                new SecureRandom().nextBytes(bytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                session.setAttribute(TOKEN, token);
            }
        }
        req.setAttribute(TOKEN, token);
        return token;
    }

    /** Không tạo token khi kiểm tra POST; thiếu/sai/trùng đều bị từ chối. */
    public static boolean isValid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        String[] values = req.getParameterValues(TOKEN);
        if (session == null || values == null || values.length != 1
                || values[0] == null || values[0].length() != 43) return false;
        String expected = (String) session.getAttribute(TOKEN);
        return expected != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), values[0].getBytes(StandardCharsets.UTF_8));
    }
}
