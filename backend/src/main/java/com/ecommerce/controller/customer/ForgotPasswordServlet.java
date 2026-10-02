package com.ecommerce.controller.customer;

import com.ecommerce.service.AuthService;
import com.ecommerce.service.EmailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// GET /forgot-password -> form nhap email;  POST -> gui email chua link dat lai mat khau
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/customer/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestBase = req.getScheme() + "://" + req.getServerName()
                + (isDefaultPort(req) ? "" : ":" + req.getServerPort()) + req.getContextPath();
        authService.requestPasswordReset(req.getParameter("email"), EmailService.resolveBaseUrl(requestBase));
        // Luon bao nhu nhau du email co ton tai hay khong (chong do email da dang ky)
        resp.sendRedirect(req.getContextPath() + "/forgot-password?sent=1");
    }

    private static boolean isDefaultPort(HttpServletRequest req) {
        int port = req.getServerPort();
        return (port == 80 && "http".equals(req.getScheme())) || (port == 443 && "https".equals(req.getScheme()));
    }
}
