package com.ecommerce.util;

import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Admin;
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
            redirectToLogin(req, resp, "/login");
            return null;
        }
        // Tai khoan bi Admin khoa trong luc dang dang nhap: user trong session la ban cu nen phai hoi lai DB, khoa thi dang xuat ngay
        User fresh = new UserDAO().findById(user.getId());
        if (fresh == null || !fresh.isActive()) {
            req.getSession().invalidate();
            redirectToLogin(req, resp, "/login");
            return null;
        }
        if (!(user instanceof Customer customer)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng này dành cho khách hàng");
            return null;
        }
        return customer;
    }

    /** Giong requireCustomer nhung cho cac trang /admin/*: chua dang nhap -> /admin/login?next=..., la khach hang -> 403. */
    public static Admin requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req);
        if (user == null) {
            redirectToLogin(req, resp, "/admin/login"); // Admin co trang dang nhap rieng
            return null;
        }
        if (!(user instanceof Admin admin)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng này dành cho quản trị viên");
            return null;
        }
        return admin;
    }

    // loginPath: trang dang nhap can chuyen toi ("/login" cho khach hang, "/admin/login" cho Admin); next = trang dang truy cap de quay lai
    private static void redirectToLogin(HttpServletRequest req, HttpServletResponse resp, String loginPath) throws IOException {
        String path = req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo());
        String login = req.getContextPath() + loginPath + "?next=" + URLEncoder.encode(path, StandardCharsets.UTF_8);
        if (HtmxUtil.isHtmx(req)) HtmxUtil.redirect(resp, login);
        else resp.sendRedirect(login);
    }
}
