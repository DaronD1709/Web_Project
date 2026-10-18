package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.ReviewService;
import com.ecommerce.util.CartCsrf;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Hiển thị thông tin chi tiết của một sản phẩm.
 * URL: GET /product?id=1
 */
@WebServlet("/product")
public class ProductDetailServlet extends HttpServlet {

    private static final int RELATED_PRODUCT_LIMIT = 4;

    private final ProductService productService = new ProductService();
    private final ReviewService reviewService = new ReviewService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Integer productId = ParamUtil.intOrNull(req.getParameter("id"));

        if (productId == null || productId <= 0) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Product product = productService.getProductById(productId);

        if (product == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Chuẩn bị CSRF token cho nút thêm vào giỏ hàng nếu người dùng đã đăng nhập.
        if (CartCsrf.prepare(req) != null) {
            resp.setHeader("Cache-Control", "no-store");
        }

        req.setAttribute("product", product);
        req.setAttribute(
                "relatedProducts",
                productService.getRelatedProducts(product, RELATED_PRODUCT_LIMIT)
        );

        // Danh gia: danh sach + diem trung binh cho moi nguoi xem; form chi hien voi khach DA MUA (canReview), kem danh gia cu de sua (myReview)
        req.setAttribute("reviews", reviewService.listFor(productId));
        double[] summary = reviewService.summary(productId);
        req.setAttribute("reviewCount", (long) summary[0]);
        req.setAttribute("avgRating", summary[1]);
        boolean isCustomer = SessionUtil.currentUser(req) instanceof Customer;
        req.setAttribute("isCustomer", isCustomer);
        if (isCustomer) {
            Customer customer = (Customer) SessionUtil.currentUser(req);
            boolean can = reviewService.canReview(customer, productId);
            req.setAttribute("canReview", can);
            if (can) req.setAttribute("myReview", reviewService.myReview(customer, productId));
        } else {
            req.setAttribute("canReview", false);
        }

        // Thong bao 1 lan sau khi gui danh gia ("ok:..." hoac "error:..."), doc xong xoa khoi session
        HttpSession session = req.getSession(false);
        String flash = session == null ? null : (String) session.getAttribute(ReviewServlet.FLASH);
        if (flash != null) {
            session.removeAttribute(ReviewServlet.FLASH);
            req.setAttribute("reviewFlashType", flash.startsWith("ok:") ? "success" : "error");
            req.setAttribute("reviewFlash", flash.substring(flash.indexOf(':') + 1));
        }

        req.getRequestDispatcher("/WEB-INF/views/customer/product-detail.jsp")
                .forward(req, resp);
    }
}