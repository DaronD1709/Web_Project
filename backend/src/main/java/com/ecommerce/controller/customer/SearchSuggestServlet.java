package com.ecommerce.controller.customer;

import com.ecommerce.dto.PageResult;
import com.ecommerce.dto.ProductFilter;
import com.ecommerce.entity.Product;
import com.ecommerce.service.ProductService;
import com.ecommerce.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Goi y tim kiem o o tim tren header: GET /search/suggest?q=... (htmx, goi sau khi khach ngung go 250ms).
// Tra ve MANH html (customer/fragments/search-suggest.jsp) liet ke toi da 6 san pham khop kem anh + gia de chon nhanh;
// go it hon 2 ky tu thi tra ve rong (JS an hop goi y). Dung lai ProductService.search nen cung quy tac khop/escape nhu trang /products.
@WebServlet("/search/suggest")
public class SearchSuggestServlet extends HttpServlet {

    private static final int LIMIT = 6;
    private static final int MIN_LENGTH = 2;

    private final ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        // 1. Doc tu khoa; qua ngan thi khong goi y
        String q = ParamUtil.trimOrNull(req.getParameter("q"));
        if (q != null && q.length() > 100) q = q.substring(0, 100);
        if (q == null || q.length() < MIN_LENGTH) {
            resp.setContentType("text/html;charset=UTF-8");
            return; // than rong -> JS an hop goi y
        }

        // 2. Tim toi da 6 san pham moi nhat khop tu khoa
        ProductFilter filter = new ProductFilter();
        filter.setKeyword(q);
        filter.setPageSize(LIMIT);
        PageResult<Product> result = productService.search(filter);

        // 3. Dua cho fragment
        req.setAttribute("q", q);
        req.setAttribute("result", result);
        req.getRequestDispatcher("/WEB-INF/views/customer/fragments/search-suggest.jsp").forward(req, resp);
    }
}
