package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.ProductService;
import com.ecommerce.service.ReviewService;
import com.ecommerce.util.CartCsrf;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Gui danh gia san pham: POST /product/review (productId, rating 1-5, comment, cartCsrfToken). CHI khach hang da dang nhap.
// Xong thi redirect (PRG) ve /product?id=..#reviews; loi du lieu thi quay lai trang do kem thong bao (luu tam o session, trang chi tiet doc roi xoa).
// Token chong gia mao form dung chung "cartCsrfToken" - ProductDetailServlet da chuan bi san cho trang chi tiet.
@WebServlet("/product/review")
public class ReviewServlet extends HttpServlet {

    public static final String FLASH = "reviewFlash"; // thong bao 1 lan sau redirect

    private final ReviewService reviewService = new ReviewService();
    private final ProductService productService = new ProductService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        // 1. Phai la khach hang da dang nhap (chua dang nhap -> /login?next=..., Admin -> 403)
        Customer customer = SessionUtil.requireCustomer(req, resp);
        if (customer == null) return;
        // 2. Token chong CSRF
        if (!CartCsrf.isValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên gửi biểu mẫu không hợp lệ. Vui lòng tải lại trang.");
            return;
        }
        Integer productId = ParamUtil.intOrNull(req.getParameter("productId"));
        if (productId == null || productId <= 0 || productService.getProductById(productId) == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 3. Luu danh gia; thanh cong/that bai deu quay ve trang san pham
        String back = req.getContextPath() + "/product?id=" + productId;
        try {
            boolean updated = reviewService.write(customer, productId,
                    ParamUtil.intOrNull(req.getParameter("rating")), req.getParameter("comment"));
            req.getSession().setAttribute(FLASH, "ok:" + (updated ? "Đã cập nhật đánh giá của bạn." : "Cảm ơn bạn đã đánh giá sản phẩm!"));
        } catch (ReviewService.ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            return;
        } catch (BusinessException e) {
            req.getSession().setAttribute(FLASH, "error:" + e.getMessage());
        }
        resp.sendRedirect(back + "#reviews");
    }
}
