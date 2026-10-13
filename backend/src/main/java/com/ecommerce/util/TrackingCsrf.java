package com.ecommerce.util;

import com.ecommerce.entity.Customer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Token của thao tác đơn hàng/thông báo; không thay token Cart/Profile/Checkout. */
public final class TrackingCsrf {
    private static final String TOKEN = "trackingCsrfToken";
    private TrackingCsrf() { }

    public static void prepare(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null || !(SessionUtil.currentUser(req) instanceof Customer)) return;
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
    }

    public static boolean isValid(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        String[] supplied = req.getParameterValues(TOKEN);
        if (session == null || supplied == null || supplied.length != 1
                || supplied[0] == null || supplied[0].length() != 43) return false;
        String expected = (String) session.getAttribute(TOKEN);
        return expected != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                supplied[0].getBytes(StandardCharsets.UTF_8));
    }
}
