package com.ecommerce.controller.customer;

import com.ecommerce.service.AuthService;
import com.ecommerce.service.BusinessException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// GET /register -> form;  POST /register -> tao tai khoan + gui email chao mung (xem docs/api-spec.md muc 2)
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/customer/register.jsp";
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String phone = req.getParameter("phone");
        try {
            authService.register(fullName, email, phone, req.getParameter("password"), req.getParameter("confirmPassword"));
            resp.sendRedirect(req.getContextPath() + "/login?registered=1"); // Post/Redirect/Get
        } catch (BusinessException e) {
            // Loi validate: hien lai form, giu gia tri da nhap (KHONG giu mat khau)
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("fullName", fullName);
            req.setAttribute("email", email);
            req.setAttribute("phone", phone);
            req.getRequestDispatcher(VIEW).forward(req, resp);
        }
    }
}
