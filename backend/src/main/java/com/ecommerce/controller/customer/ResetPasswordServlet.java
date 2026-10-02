package com.ecommerce.controller.customer;

import com.ecommerce.service.AuthService;
import com.ecommerce.service.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// GET /reset-password?token=...  -> form mat khau moi (hoac bao link het han);  POST -> doi mat khau
@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/customer/reset-password.jsp";
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        req.setAttribute("token", token);
        req.setAttribute("invalid", !authService.isResetTokenValid(token));
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        try {
            authService.resetPassword(token, req.getParameter("password"), req.getParameter("confirmPassword"));
            resp.sendRedirect(req.getContextPath() + "/login?reset=1");
        } catch (BusinessException e) {
            req.setAttribute("token", token);
            req.setAttribute("errors", e.getErrors());
            // Token sai/het han: errors rong -> hien thong bao chung cua exception
            req.setAttribute("invalid", e.getErrors().isEmpty());
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher(VIEW).forward(req, resp);
        }
    }
}
