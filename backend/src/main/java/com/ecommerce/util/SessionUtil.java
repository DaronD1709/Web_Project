package com.ecommerce.util;

import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Admin;
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
    private static void redirectToLogin(HttpServletRequest req,
                                        HttpServletResponse resp,
                                        String loginPath) throws IOException {
        String path = req.getServletPath()
                + (req.getPathInfo() == null ? "" : req.getPathInfo());

        if ("GET".equals(req.getMethod())) {
            // Giữ query để đăng nhập xong vẫn xem đúng đơn hàng.
            String query = req.getQueryString();
            if (query != null && !query.isBlank()) {
                path += "?" + query;
            }
        } else if ("/orders/cancel".equals(path) || "/orders/return".equals(path)) {
            Integer id = ParamUtil.intOrNull(req.getParameter("id"));
            path = id != null && id > 0 ? "/orders/detail?id=" + id : "/orders";
        } else if ("/account/password".equals(path)) {
            // Sau login trinh duyet gui GET; endpoint doi mat khau chi nhan POST nen quay ve trang ho so.
            path = "/account/profile";
        } else if ("/product/review".equals(path)) {
            // Gui danh gia luc het phien: /product/review chi nhan POST nen sau login khong quay lai duoc (405).
            // Ve trang chi tiet san pham de lay form + token moi.
            Integer productId = ParamUtil.intOrNull(req.getParameter("productId"));
            path = productId != null && productId > 0 ? "/product?id=" + productId + "#reviews" : "/products";
        } else if ("/notifications/read".equals(path)) {
            path = "/notifications";
        } else if ("/checkout/voucher".equals(path)) {
            // Login xong trình duyệt gửi GET; quay về trang checkout.
            path = "/checkout";
        } else if ("/payment/vnpay/reconcile".equals(path)) {
            // Đối soát chỉ POST: sau login quay về GET kết quả để lấy token mới.
            Integer orderId = ParamUtil.intOrNull(req.getParameter("orderId"));
            path = "/checkout/success";
            if (orderId != null && orderId > 0) {
                path += "?orderId=" + orderId;
            }
        }

        String login = req.getContextPath() + loginPath + "?next="
                + URLEncoder.encode(path, StandardCharsets.UTF_8);

        if (HtmxUtil.isHtmx(req)) {
            HtmxUtil.redirect(resp, login);
        } else {
            resp.sendRedirect(login);
        }
    }
}
