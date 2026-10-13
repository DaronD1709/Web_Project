package com.ecommerce.controller.customer;

import com.ecommerce.entity.User;
import com.ecommerce.service.AuthService;
import com.ecommerce.entity.Customer;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.CartService;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

// GET /login -> form;  POST /login -> kiem tra mat khau, luu "currentUser" vao session
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/customer/login.jsp";
    private final AuthService authService = new AuthService();
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        req.setAttribute("next", req.getParameter("next"));
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        String email = req.getParameter("email");
        String next = req.getParameter("next");
        boolean remember = "on".equals(req.getParameter("remember"));
        try {
            User user = authService.login(email, req.getParameter("password"));
            // Doc du lieu can thiet truoc khi tao phien dang nhap; loi DB khong de lai phien da xac thuc mot phan.
            long cartCount = user instanceof Customer ? cartService.countItems(user.getId()) : 0;
            HttpSession previous = req.getSession(false);
            if (previous != null) previous.invalidate();
            HttpSession session = req.getSession(true); // phien moi: doi ID va bo du lieu cua tai khoan truoc
            session.setAttribute(SessionUtil.CURRENT_USER, user);
            if (user instanceof Customer) { // badge gio hang tren header
                session.setAttribute(SessionUtil.CART_COUNT, cartCount);
            }
            if (remember) session.setMaxInactiveInterval(SessionUtil.REMEMBER_SECONDS);
            SessionUtil.writeSessionCookie(req, resp, session.getId(), remember ? SessionUtil.REMEMBER_SECONDS : -1);
            resp.sendRedirect(req.getContextPath() + (isSafeInternalPath(next) ? next : "/"));
        } catch (BusinessException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("email", email);
            req.setAttribute("next", next);
            req.setAttribute("remember", remember);
            req.getRequestDispatcher(VIEW).forward(req, resp);
        }
    }

    // Chi cho chuyen huong toi duong dan NOI BO (vd "/orders"); chan "//evil.com", "https://evil.com" (open redirect).
    private static boolean isSafeInternalPath(String next) {
        return next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\")
                && next.chars().noneMatch(Character::isISOControl);
    }
}
