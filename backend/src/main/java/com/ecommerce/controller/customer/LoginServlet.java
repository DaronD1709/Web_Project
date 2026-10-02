package com.ecommerce.controller.customer;

import com.ecommerce.entity.User;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// GET /login -> form;  POST /login -> kiem tra mat khau, luu "currentUser" vao session
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/customer/login.jsp";
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("next", req.getParameter("next"));
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String next = req.getParameter("next");
        try {
            User user = authService.login(email, req.getParameter("password"));
            if (req.getSession(false) != null) {
                req.changeSessionId(); // doi ma session luc dang nhap -> chong session fixation
            }
            req.getSession(true).setAttribute("currentUser", user);
            resp.sendRedirect(req.getContextPath() + (isSafeInternalPath(next) ? next : "/products"));
        } catch (BusinessException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("email", email);
            req.setAttribute("next", next);
            req.getRequestDispatcher(VIEW).forward(req, resp);
        }
    }

    // Chi cho chuyen huong toi duong dan NOI BO (vd "/orders"); chan "//evil.com", "https://evil.com" (open redirect).
    private static boolean isSafeInternalPath(String next) {
        return next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\");
    }
}
