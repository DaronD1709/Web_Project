package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Order;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.VNPayService;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import com.ecommerce.util.VNPaySignature;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@WebServlet("/payment/vnpay/return")
public class VNPayReturnServlet extends HttpServlet {

    private final OrderService orderService = new OrderService();
    private final VNPayService vnPayService = new VNPayService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Referrer-Policy", "no-referrer");

        // Return đi qua trình duyệt khách nên cần kiểm tra đăng nhập.
        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) {
            return;
        }

        Map<String, String> fields;
        try {
            fields = VNPaySignature.readFields(req.getParameterMap());
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!vnPayService.verifyResponse(fields)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Không xác thực được phản hồi VNPay.");
            return;
        }

        Integer orderId = ParamUtil.intOrNull(fields.get("vnp_TxnRef"));
        if (orderId == null || orderId < 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Order order;
        try {
            // Service kiểm tra đơn có thuộc đúng khách hay không.
            order = orderService.getOwnOrder(user, orderId);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            vnPayService.validateReturnOrder(order, fields);
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    e.getMessage());
            return;
        }

        // Chỉ hiển thị trạng thái đã lưu; không gọi processIpn ở Return.
        // Redirect giúp URL không giữ các tham số gateway.
        resp.sendRedirect(req.getContextPath()
                + "/checkout/success?orderId=" + orderId);
    }
}