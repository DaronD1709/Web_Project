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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Giỏ hàng phụ thuộc tài khoản, không lưu response vào cache.
        resp.setHeader("Cache-Control", "no-store");

        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) return;
        showCart(req, user);

        // Đồng bộ badge với DB mỗi khi khách mở lại trang giỏ.
        refreshCartCount(req, user);

        req.getRequestDispatcher(PAGE).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) return;
        boolean htmx = HtmxUtil.isHtmx(req);
        String action = req.getParameter("action");
        String message = null;

        try {
            switch (action == null ? "" : action) {
                case "add" -> {
                    int productId = readIntParameter(
                            req, "productId", "Mã sản phẩm");
                    int qty = readIntParameter(
                            req, "qty", "Số lượng");

                    // Chủ giỏ lấy từ session đã xác thực,
                    // không lấy customerId do trình duyệt gửi lên.
                    cartService.addItem(user.getId(), productId, qty);
                    message = "Đã thêm vào giỏ hàng";
                }

                case "update" -> {
                    int itemId = readIntParameter(
                            req, "itemId", "Mã dòng giỏ hàng");
                    int qty = readIntParameter(
                            req, "qty", "Số lượng");

                    cartService.updateQuantity(user.getId(), itemId, qty);
                }

                case "remove" -> {
                    int itemId = readIntParameter(
                            req, "itemId", "Mã dòng giỏ hàng");

                    cartService.removeItem(user.getId(), itemId);
                    message = "Đã xoá khỏi giỏ hàng";
                }

                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (BusinessException e) {
            if (htmx) {
                // 422: dữ liệu không hợp lệ, theo docs/api-spec.md.
                resp.setStatus(422);

                // Báo lỗi bằng toast, giữ nguyên nội dung giỏ hiện tại.
                HtmxUtil.toast(resp, e.getMessage(), "error");
                resp.setHeader("HX-Reswap", "none");
                return;
            }

            // Request thường: dựng lại trang giỏ cùng thông báo lỗi.
            req.setAttribute("error", e.getMessage());
            showCart(req, user);
            refreshCartCount(req, user);
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

    private int readIntParameter(HttpServletRequest req,
                                  String parameterName,
                                  String fieldLabel) {
        // Tham số HTTP là chuỗi; ParamUtil trả null nếu thiếu,
        // sai định dạng hoặc vượt phạm vi int.
        Integer value = ParamUtil.intOrNull(
                req.getParameter(parameterName));

        // Không tự thay dữ liệu sai bằng số mặc định khi sửa giỏ.
        if (value == null) {
            throw new BusinessException(
                    fieldLabel + " phải là số nguyên hợp lệ.");
        }
        return value;
    }

    private void refreshCartCount(HttpServletRequest req, Customer user) {
        req.getSession().setAttribute(SessionUtil.CART_COUNT, cartService.countItems(user.getId()));
    }
}
