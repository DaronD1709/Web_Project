package com.ecommerce.controller.customer;

import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

// Chi nhan POST (nut "Dang xuat" la 1 form nho) de link ben ngoai khong tu dong dang xuat nguoi dung.
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        SessionUtil.writeSessionCookie(req, resp, "", 0); // xoa ca cookie cua phien ghi nho
        resp.sendRedirect(req.getContextPath() + "/");
    }
}
