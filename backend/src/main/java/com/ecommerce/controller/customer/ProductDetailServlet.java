package com.ecommerce.controller.customer;

import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import com.ecommerce.util.CartCsrf;
import com.ecommerce.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Hiển thị thông tin chi tiết của một sản phẩm.
 * URL: GET /product?id=1
 */
@WebServlet("/product")
public class ProductDetailServlet extends HttpServlet {

    private static final int RELATED_PRODUCT_LIMIT = 4;

    private final ProductService productService = new ProductService();

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

        /*
         * Product Review đang thuộc nhánh riêng của thành viên khác.
         * Giữ đúng tên attribute để JSP và nhánh Product Review tích hợp sau.
         */
        req.setAttribute("reviews", List.of());
        req.setAttribute("avgRating", 0.0);
        req.setAttribute("canReview", false);

        req.getRequestDispatcher("/WEB-INF/views/customer/product-detail.jsp")
                .forward(req, resp);
    }
}