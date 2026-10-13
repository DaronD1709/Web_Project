package com.ecommerce.controller.customer;

import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Voucher;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.VoucherService;
import com.ecommerce.util.HtmxUtil;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/checkout/voucher")
public class VoucherServlet extends HttpServlet {

    private static final String FRAGMENT =
            "/WEB-INF/views/customer/fragments/checkout-voucher.jsp";

    private final CartService cartService = new CartService();
    private final OrderService orderService = new OrderService();
    private final VoucherService voucherService = new VoucherService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) {
            return;
        }

        String code = ParamUtil.trimOrNull(req.getParameter("code"));
        Cart cart = cartService.getCart(user.getId());
        double subtotal = cart.calculateTotal();
        double shipping = cartService.getShippingFee(subtotal);

        Voucher voucher = null;
        double discount = 0;
        String error = null;

        try {
            if (cart.getItems().isEmpty()) {
                throw new BusinessException(
                        "Giỏ hàng của bạn đang trống.");
            }

            voucher = voucherService.getByCode(code);
            discount = voucherService.calculateDiscount(voucher, subtotal);
        } catch (BusinessException e) {
            // Voucher lỗi: phản hồi không giữ mức giảm của mã cũ.
            voucher = null;
            discount = 0;
            error = e.getMessage();
        }

        if (!HtmxUtil.isHtmx(req)) {
            // POST thường chuyển về GET; bước sau sẽ đọc mã ở URL.
            String encodedCode = URLEncoder.encode(
                    code == null ? "" : code,
                    StandardCharsets.UTF_8);

            resp.sendRedirect(req.getContextPath()
                    + "/checkout?voucherCode=" + encodedCode);
            return;
        }

        req.setAttribute("cart", cart);
        req.setAttribute("addresses",
                orderService.getAddresses(user.getId()));
        req.setAttribute("subtotal", subtotal);
        req.setAttribute("shippingFee", shipping);
        req.setAttribute("discountAmount", discount);
        req.setAttribute("total", subtotal - discount + shipping);
        req.setAttribute("voucher", voucher);
        req.setAttribute("voucherError", error);
        req.setAttribute("voucherCode", code == null ? "" : code);
        req.setAttribute("appliedVoucherCode",
                voucher == null ? "" : voucher.getCode());

        resp.setStatus(error == null
                ? HttpServletResponse.SC_OK : 422);
        req.getRequestDispatcher(FRAGMENT).forward(req, resp);
    }
}