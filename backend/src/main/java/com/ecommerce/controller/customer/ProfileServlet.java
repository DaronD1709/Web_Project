package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.UserService;
import com.ecommerce.util.HtmxUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@WebServlet({"/account/profile", "/account/password"})
public class ProfileServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/customer/profile.jsp";
    private static final String CSRF_TOKEN = "profileCsrfToken";
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        if (!"/account/profile".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Customer customer = SessionUtil.requireCustomer(req, resp);
        if (customer == null) return;
        try {
            Customer fresh = userService.getProfile(customer.getId());
            req.getSession(false).setAttribute(SessionUtil.CURRENT_USER, fresh);
            prepareView(req, fresh);
            req.getRequestDispatcher(VIEW).forward(req, resp);
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        Customer customer = SessionUtil.requireCustomer(req, resp);
        if (customer == null) return;
        try {
            customer = userService.getProfile(customer.getId());
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String expected = (String) req.getSession(false).getAttribute(CSRF_TOKEN);
        String supplied = req.getParameter("csrfToken");
        if (expected == null || supplied == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên gửi biểu mẫu không hợp lệ. Vui lòng tải lại trang.");
            return;
        }
        boolean passwordForm = "/account/password".equals(req.getServletPath());
        boolean htmx = HtmxUtil.isHtmx(req);
        try {
            Customer updated = passwordForm
                    ? userService.changePassword(customer.getId(), req.getParameter("current"),
                            req.getParameter("newPassword"), req.getParameter("confirm"))
                    : userService.updateProfile(customer.getId(), req.getParameter("fullName"), req.getParameter("phone"));
            req.getSession(false).setAttribute(SessionUtil.CURRENT_USER, updated);
            if (!htmx) {
                resp.sendRedirect(req.getContextPath() + "/account/profile?" + (passwordForm ? "passwordChanged=1" : "saved=1"));
                return;
            }
            prepareView(req, updated);
            req.setAttribute(passwordForm ? "passwordChanged" : "saved", true);
            req.setAttribute("updateHeader", !passwordForm);
            HtmxUtil.toast(resp, passwordForm ? "Đã cập nhật mật khẩu." : "Đã lưu thay đổi.", "success");
        } catch (BusinessException e) {
            prepareView(req, customer);
            req.setAttribute(passwordForm ? "passwordErrors" : "profileErrors", e.getErrors());
            req.setAttribute(passwordForm ? "passwordError" : "profileError", e.getErrors().isEmpty() ? e.getMessage() : null);
            if (!passwordForm) {
                req.setAttribute("fullName", req.getParameter("fullName"));
                req.setAttribute("phone", req.getParameter("phone"));
            }
            if (htmx) resp.setStatus(422);
        }
        String fragment = passwordForm ? "profile-password.jsp" : "profile-info.jsp";
        req.getRequestDispatcher(htmx ? "/WEB-INF/views/customer/fragments/" + fragment : VIEW).forward(req, resp);
    }

    private void prepareView(HttpServletRequest req, Customer user) {
        HttpSession session = req.getSession(false);
        String token = (String) session.getAttribute(CSRF_TOKEN);
        if (token == null) {
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CSRF_TOKEN, token);
        }
        req.setAttribute("csrfToken", token);
        req.setAttribute("user", user);
        req.setAttribute("fullName", user.getFullName());
        req.setAttribute("phone", user.getPhone());
        String name = user.getFullName();
        String initials = "KH";
        if (name != null && !name.isBlank()) {
            String[] words = name.trim().split("\\s+");
            initials = firstLetter(words[0]);
            if (words.length > 1) initials += firstLetter(words[words.length - 1]);
            initials = initials.toUpperCase(Locale.ROOT);
        }
        req.setAttribute("initials", initials);
        req.setAttribute("joinedMonth", user.getCreatedAt() == null ? "" : user.getCreatedAt().format(DateTimeFormatter.ofPattern("MM/yyyy")));
    }

    private static String firstLetter(String word) {
        return word.substring(0, word.offsetByCodePoints(0, 1));
    }
}
