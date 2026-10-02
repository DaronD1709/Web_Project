package com.ecommerce.controller.customer;

import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Customer;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.CartService;
import com.ecommerce.util.HtmxUtil;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// GET /cart -> trang gio hang;  POST /cart  action=add|update|remove  (xem docs/api-spec.md muc 4)
// Request tu htmx tra ve 1 DOAN HTML nho (khong reload trang); request thuong thi redirect (Post/Redirect/Get).
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    private static final String PAGE = "/WEB-INF/views/customer/cart.jsp";
    private static final String FRAG_COUNT = "/WEB-INF/views/customer/fragments/cart-count.jsp";
    private static final String FRAG_UPDATED = "/WEB-INF/views/customer/fragments/cart-updated.jsp";

    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) return;
        showCart(req, user);
        req.getRequestDispatcher(PAGE).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) return;
        boolean htmx = HtmxUtil.isHtmx(req);
        String action = req.getParameter("action");
        String message = null;

        try {
            switch (action == null ? "" : action) {
                case "add" -> {
                    cartService.addItem(user.getId(), ParamUtil.intOr(req.getParameter("productId"), -1), ParamUtil.intOr(req.getParameter("qty"), 1));
                    message = "Đã thêm vào giỏ hàng";
                }
                case "update" -> cartService.updateQuantity(user.getId(), ParamUtil.intOr(req.getParameter("itemId"), -1), ParamUtil.intOr(req.getParameter("qty"), 1));
                case "remove" -> {
                    cartService.removeItem(user.getId(), ParamUtil.intOr(req.getParameter("itemId"), -1));
                    message = "Đã xoá khỏi giỏ hàng";
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (BusinessException e) {
            // Vd: vuot ton kho. htmx -> bao toast loi, giu nguyen giao dien; thuong -> quay lai gio kem thong bao.
            if (htmx) {
                HtmxUtil.toast(resp, e.getMessage(), "error");
                resp.setHeader("HX-Reswap", "none");
                return;
            }
            req.setAttribute("error", e.getMessage());
            showCart(req, user);
            req.getRequestDispatcher(PAGE).forward(req, resp);
            return;
        }

        refreshCartCount(req, user);
        if (!htmx) {
            resp.sendRedirect(req.getContextPath() + "/cart");
            return;
        }
        if (message != null) HtmxUtil.toast(resp, message, "success");
        if ("add".equals(action)) {
            req.getRequestDispatcher(FRAG_COUNT).forward(req, resp);      // chi doi badge o header
        } else {
            showCart(req, user);
            req.setAttribute("oob", true);                                // kem badge cap nhat (hx-swap-oob)
            req.getRequestDispatcher(FRAG_UPDATED).forward(req, resp);    // doi noi dung gio hang + badge
        }
    }

    private void showCart(HttpServletRequest req, Customer user) {
        Cart cart = cartService.getCart(user.getId());
        double subtotal = cart.calculateTotal();
        double shipping = cartService.getShippingFee(subtotal);
        req.setAttribute("cart", cart);
        req.setAttribute("subtotal", subtotal);
        req.setAttribute("shippingFee", shipping);
        req.setAttribute("total", subtotal + shipping);
    }

    private void refreshCartCount(HttpServletRequest req, Customer user) {
        req.getSession().setAttribute(SessionUtil.CART_COUNT, cartService.countItems(user.getId()));
    }
}
