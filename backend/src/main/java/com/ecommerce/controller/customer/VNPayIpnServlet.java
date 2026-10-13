package com.ecommerce.controller.customer;

import com.ecommerce.service.VNPayService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Nhận kết quả server-to-server từ VNPay. */
@WebServlet("/payment/vnpay/ipn")
public class VNPayIpnServlet extends HttpServlet {

    private final VNPayService vnPayService = new VNPayService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");

        // Máy chủ VNPay không có session đăng nhập Customer.
        // Service xác thực bằng chữ ký và mã merchant.
        String code;
        try {
            code = vnPayService.processIpn(req.getParameterMap());
        } catch (RuntimeException e) {
            code = "99";
        }

        String message = switch (code) {
            case "00" -> "Confirm Success";
            case "01" -> "Order not found";
            case "02" -> "Order already confirmed";
            case "04" -> "Invalid amount";
            case "97" -> "Invalid signature";
            default -> {
                code = "99";
                yield "Unknown error";
            }
        };

        // HTTP 200 trả giao thức IPN; RspCode là kết quả xử lý.
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{\"RspCode\":\"" + code
                + "\",\"Message\":\"" + message + "\"}");
    }
}