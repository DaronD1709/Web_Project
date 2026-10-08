package com.ecommerce.util;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.User;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Doc nguoi dung dang dang nhap tu session (tam thoi, truoc khi co AuthFilter o feat/auth-filter). */
public class SessionUtil {

    public static final String CURRENT_USER = "currentUser";
    public static final String CART_COUNT = "cartCount";
    public static final int REMEMBER_SECONDS = 7 * 24 * 60 * 60;

    /** Cookie phien: maxAge=-1 la phien thuong, 7 ngay khi ghi nho, 0 de xoa khi dang xuat. */
    public static void writeSessionCookie(HttpServletRequest req, HttpServletResponse resp, String value, int maxAge) {
        SessionCookieConfig config = req.getServletContext().getSessionCookieConfig();
        String name = config.getName() == null ? "JSESSIONID" : config.getName();
        Cookie cookie = new Cookie(name, value);
        String path = config.getPath();
        cookie.setPath(path != null ? path : (req.getContextPath().isEmpty() ? "/" : req.getContextPath()));
        if (config.getDomain() != null) cookie.setDomain(config.getDomain());
        cookie.setHttpOnly(true);
        cookie.setSecure(req.isSecure() || config.isSecure());
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge(maxAge);
        resp.addCookie(cookie);
    }

    public static User currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (User) s.getAttribute(CURRENT_USER);
    }

    /**
     * Dung cho cac trang chi danh cho KHACH HANG da dang nhap. Chua dang nhap -> chuyen toi /login?next=...
     * (htmx: dung header HX-Redirect), la Admin -> 403. Tra ve null neu da tra loi, Servlet chi viec `return`.
     */
    public static Customer requireCustomer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req);
        if (user == null) {
            String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());
            String login = req.getContextPath() + "/login?next=" + URLEncoder.encode(path, StandardCharsets.UTF_8);
            if (HtmxUtil.isHtmx(req)) HtmxUtil.redirect(resp, login);
            else resp.sendRedirect(login);
            return null;
        }
        if (!(user instanceof Customer customer)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng này dành cho khách hàng");
            return null;
        }
        return customer;
    }
}
