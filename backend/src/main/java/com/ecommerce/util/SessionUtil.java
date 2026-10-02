package com.ecommerce.util;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.User;
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
