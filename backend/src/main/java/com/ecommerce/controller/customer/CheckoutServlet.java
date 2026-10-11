package com.ecommerce.controller.customer;

import com.ecommerce.entity.Address;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Order;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.CartService;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import com.ecommerce.entity.Voucher;
import com.ecommerce.service.VoucherService;
import com.ecommerce.entity.VNPayPayment;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@WebServlet({"/checkout", "/checkout/success", "/payment/vnpay/start", "/payment/vnpay/reconcile"})
public class CheckoutServlet extends HttpServlet {

    private static final String PAGE =
            "/WEB-INF/views/customer/checkout.jsp";
    private static final String SUCCESS_PAGE =
            "/WEB-INF/views/customer/order-success.jsp";
    private static final String CHECKOUT_TOKEN = "checkoutToken";
    private static final String RECONCILE_TOKEN = "vnPayReconcileToken";
    private static final String RECONCILE_NEXT = "vnPayReconcileNext";
    private static final String RECONCILE_FLASH = "vnPayReconcileFlash";
    private static final Logger LOG = Logger.getLogger(CheckoutServlet.class.getName());

    private record PaymentFlash(int orderId, String message, boolean error)
            implements java.io.Serializable {
    }

    private static final String COMPLETED_TOKEN = "checkoutCompletedToken";
    private static final String COMPLETED_ORDER = "checkoutCompletedOrderId";

    private final VoucherService voucherService = new VoucherService();

    private final CartService cartService = new CartService();
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("Referrer-Policy", "no-referrer");

        if ("/payment/vnpay/reconcile".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) {
            return;
        }

        // Chuyển đơn đã tạo sang cổng thanh toán, không tạo thêm đơn.
        if ("/payment/vnpay/start".equals(req.getServletPath())) {
            startVNPay(req, resp, user);
            return;
        }

        if ("/checkout/success".equals(req.getServletPath())) {
            showSuccess(req, resp, user);
            return;
        }

        if (prepareCheckout(req, resp, user)) {
            req.getRequestDispatcher(PAGE).forward(req, resp);
        }
    }

    private boolean prepareCheckout(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    Customer user) throws IOException {
        Cart cart = cartService.getCart(user.getId());

        if (cart.getItems().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/cart");
            return false;
        }

        List<Address> addresses = orderService.getAddresses(user.getId());
        Address defaultAddress = addresses.isEmpty()
                ? null : addresses.get(0);

        double subtotal = cart.calculateTotal();
        double shipping = cartService.getShippingFee(subtotal);

        // JSP chỉ cho chọn VNPay khi cấu hình môi trường đang dùng sẵn sàng.
        req.setAttribute("vnPayAvailable", orderService.isVNPayAvailable());
        req.setAttribute("vnPaySandbox", orderService.isVNPaySandbox());

        req.setAttribute("cart", cart);
        req.setAttribute("addresses", addresses);
        req.setAttribute("defaultAddress", defaultAddress);
        req.setAttribute("subtotal", subtotal);
        req.setAttribute("shippingFee", shipping);
        prepareVoucher(req, subtotal, shipping);

        if (req.getAttribute("selectedAddressId") == null) {
            req.setAttribute("selectedAddressId",
                    defaultAddress == null ? null : defaultAddress.getId());
        }

        req.getSession().setAttribute(
                SessionUtil.CART_COUNT,
                cartService.countItems(user.getId()));

        // Token gắn với phiên đăng nhập, sẽ kiểm tra khi POST đặt hàng.
        HttpSession session = req.getSession();
        synchronized (session) {
            String token = (String) session.getAttribute(CHECKOUT_TOKEN);
            if (token == null) {
                token = UUID.randomUUID().toString();
                session.setAttribute(CHECKOUT_TOKEN, token);
            }
            req.setAttribute(CHECKOUT_TOKEN, token);
        }

        return true;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        if ("/payment/vnpay/reconcile".equals(req.getServletPath())) {
            Customer user = SessionUtil.requireCustomer(req, resp);
            if (user != null) {
                reconcileVNPay(req, resp, user);
            }
            return;
        }

        // Trang hoàn tất chỉ nhận GET.
        if (!"/checkout".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) {
            return;
        }

        Integer addressId = ParamUtil.intOrNull(req.getParameter("addressId"));
        String method = req.getParameter("paymentMethod");
        String voucherCode = ParamUtil.trimOrNull(
                req.getParameter("voucherCode"));
        String submittedToken = req.getParameter(CHECKOUT_TOKEN);

        HttpSession session = req.getSession();
        Integer successfulOrderId = null;
        String error = null;

        // Phối hợp các request đặt hàng trong cùng phiên đăng nhập.
        synchronized (session) {
            String completedToken =
                    (String) session.getAttribute(COMPLETED_TOKEN);

            if (submittedToken != null
                    && submittedToken.equals(completedToken)) {
                // Form này đã thành công: dùng lại đơn đã tạo.
                successfulOrderId =
                        (Integer) session.getAttribute(COMPLETED_ORDER);
            } else {
                String expectedToken =
                        (String) session.getAttribute(CHECKOUT_TOKEN);

                if (expectedToken == null || submittedToken == null
                        || !expectedToken.equals(submittedToken)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Phiên đặt hàng không hợp lệ. Vui lòng mở lại trang thanh toán.");
                    return;
                }

                try {
                    Order order = orderService.checkout(
                            user, addressId, method, voucherCode);
                    successfulOrderId = order.getId();

                    // Chỉ đánh dấu thành công sau khi Service đã commit.
                    session.setAttribute(COMPLETED_TOKEN, submittedToken);
                    session.setAttribute(COMPLETED_ORDER, successfulOrderId);
                    session.removeAttribute(CHECKOUT_TOKEN);
                } catch (BusinessException e) {
                    // Giữ token để khách sửa dữ liệu rồi gửi lại.
                    error = e.getMessage();
                }
            }
        }

        if (successfulOrderId != null) {
            session.setAttribute(
                    SessionUtil.CART_COUNT,
                    cartService.countItems(user.getId()));

            // Dùng phương thức của đơn đã lưu.
            // Gửi lại form dù đổi paymentMethod cũng vẫn quay về đúng đơn cũ.
            Order saved = orderService.getOwnOrder(user, successfulOrderId);

            String destination = saved.getPayment() instanceof VNPayPayment
                    ? "/payment/vnpay/start?orderId="
                    : "/checkout/success?orderId=";

            resp.sendRedirect(req.getContextPath() + destination + successfulOrderId);
            return;
        }

        // Khi form lỗi, giữ lựa chọn VNPay nếu vẫn khả dụng.
        req.setAttribute("selectedPaymentMethod",
                "VNPAY".equals(method) && orderService.isVNPayAvailable()
                        ? "VNPAY" : "COD");

        req.setAttribute("error", error);
        req.setAttribute("selectedAddressId", addressId);
        req.setAttribute("voucherCode",
                voucherCode == null ? "" : voucherCode);

        if (prepareCheckout(req, resp, user)) {
            req.getRequestDispatcher(PAGE).forward(req, resp);
        }
    }

    private void startVNPay(HttpServletRequest req,
                            HttpServletResponse resp,
                            Customer user)
            throws ServletException, IOException {

        Integer orderId = ParamUtil.intOrNull(req.getParameter("orderId"));
        if (orderId == null || orderId < 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            // Service kiểm tra chủ đơn, trạng thái và thời hạn thanh toán.
            String url = orderService.getVNPayPaymentUrl(
                    user, orderId, req.getRemoteAddr());

            resp.sendRedirect(url);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (BusinessException e) {
            // Đơn đã tồn tại: hiển thị trạng thái mới nhất.
            // Không tạo đơn khác hoặc tự kết luận thanh toán thất bại.
            req.setAttribute("paymentError", e.getMessage());
            showSuccess(req, resp, user);
        }
    }

    private void showSuccess(HttpServletRequest req,
                             HttpServletResponse resp,
                             Customer user)
            throws ServletException, IOException {
        Integer orderId = ParamUtil.intOrNull(req.getParameter("orderId"));

        if (orderId == null || orderId < 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Mã đơn hàng không hợp lệ.");
            return;
        }

        try {
            Order order = orderService.getOwnOrder(user, orderId);
            req.setAttribute("order", order);
            req.setAttribute("vnPayAvailable", orderService.isVNPayAvailable());
            HttpSession session = req.getSession();
            synchronized (session) {
                if (session.getAttribute(RECONCILE_TOKEN) == null) {
                    session.setAttribute(RECONCILE_TOKEN, UUID.randomUUID().toString());
                }
                req.setAttribute(RECONCILE_TOKEN, session.getAttribute(RECONCILE_TOKEN));
                PaymentFlash flash = (PaymentFlash) session.getAttribute(RECONCILE_FLASH);
                if (flash != null && flash.orderId() == orderId) {
                    req.setAttribute(flash.error() ? "paymentError" : "paymentMessage", flash.message());
                    session.removeAttribute(RECONCILE_FLASH);
                }
            }
            req.getRequestDispatcher(SUCCESS_PAGE).forward(req, resp);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void reconcileVNPay(HttpServletRequest req,
            HttpServletResponse resp, Customer user) throws IOException {
        Integer orderId = ParamUtil.intOrNull(req.getParameter("orderId"));
        String[] ids = req.getParameterValues("orderId");
        String[] tokens = req.getParameterValues(RECONCILE_TOKEN);
        if (orderId == null || orderId < 1 || ids == null || ids.length != 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        HttpSession session = req.getSession();
        String message = null;
        boolean error = false;
        synchronized (session) {
            String expected = (String) session.getAttribute(RECONCILE_TOKEN);
            if (expected == null || tokens == null || tokens.length != 1
                    || !expected.equals(tokens[0])) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            Long next = (Long) session.getAttribute(RECONCILE_NEXT);
            long now = System.currentTimeMillis();
            if (next != null && now < next) {
                message = "Vui lòng chờ 5 phút giữa các lần kiểm tra thanh toán.";
                error = true;
            } else {
                session.setAttribute(RECONCILE_NEXT, now + 300_000L);
            }
        }
        if (message == null) {
            try {
                // Không giữ khoá session hoặc transaction DB khi gọi gateway.
                orderService.reconcileVNPay(user, orderId);
                message = "Đã kiểm tra thanh toán. Kết quả mới nhất được hiển thị bên dưới.";
            } catch (SecurityException e) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            } catch (BusinessException e) {
                message = e.getMessage();
                error = true;
            } catch (RuntimeException e) {
                LOG.warning("Chưa đối soát được đơn #" + orderId + ": " + e.getClass().getSimpleName());
                message = "Chưa kiểm tra được thanh toán. Vui lòng thử lại sau hoặc liên hệ cửa hàng.";
                error = true;
            }
        }
        session.setAttribute(RECONCILE_FLASH, new PaymentFlash(orderId, message, error));
        resp.sendRedirect(req.getContextPath() + "/checkout/success?orderId=" + orderId);
    }

    private void prepareVoucher(HttpServletRequest req,
                                double subtotal,
                                double shipping) {
        // Khi POST đặt hàng bị lỗi, giữ mã khách đã gửi.
        // Khi áp dụng bằng POST thường, mã nằm trên URL GET checkout.
        String code = (String) req.getAttribute("voucherCode");
        if (code == null) {
            code = req.getParameter("voucherCode");
        }
        code = ParamUtil.trimOrNull(code);

        Voucher voucher = null;
        double discount = 0;
        String error = null;

        // GET có voucherCode rỗng là lần bấm Áp dụng nhưng chưa nhập mã.
        boolean shouldPreview = code != null
                || ("GET".equals(req.getMethod())
                    && req.getParameter("voucherCode") != null);

        if (shouldPreview) {
            try {
                voucher = voucherService.getByCode(code);
                discount = voucherService.calculateDiscount(voucher, subtotal);
            } catch (BusinessException e) {
                voucher = null;
                discount = 0;
                error = e.getMessage();
            }
        }

        req.setAttribute("voucher", voucher);
        req.setAttribute("voucherError", error);
        req.setAttribute("voucherCode", code == null ? "" : code);
        req.setAttribute("appliedVoucherCode",
                voucher == null ? "" : voucher.getCode());
        req.setAttribute("discountAmount", discount);
        req.setAttribute("total", subtotal - discount + shipping);
    }
}
