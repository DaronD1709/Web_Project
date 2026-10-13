package com.ecommerce.controller.customer;

import com.ecommerce.dto.ProductFilter;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import com.ecommerce.util.CartCsrf;
import com.ecommerce.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// GET /products?cat=&q=&min=&max=&instock=on&sort=new|asc|desc&page=
@WebServlet("/products")
public class ProductServlet extends HttpServlet {

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (CartCsrf.prepare(req) != null) {
            resp.setHeader("Cache-Control", "no-store");
        }

        // Đọc các tham số lọc từ request.
        ProductFilter filter = new ProductFilter();
        filter.setCategoryId(ParamUtil.intOrNull(req.getParameter("cat")));
        filter.setKeyword(ParamUtil.trimOrNull(req.getParameter("q")));
        filter.setMinPrice(ParamUtil.doubleOrNull(req.getParameter("min")));
        filter.setMaxPrice(ParamUtil.doubleOrNull(req.getParameter("max")));
        filter.setInStockOnly("on".equals(req.getParameter("instock")));
        filter.setSort(req.getParameter("sort"));
        filter.setPage(ParamUtil.intOr(req.getParameter("page"), 1));

        // Gọi Service và chuyển dữ liệu sang JSP.
        req.setAttribute("filter", filter);
        req.setAttribute("result", productService.search(filter));
        req.setAttribute("categories", categoryService.getAllCategories());

        // Danh mục hiện được chọn, dùng để hiển thị tiêu đề trang.
        req.setAttribute(
                "selectedCategory",
                categoryService.getCategoryById(filter.getCategoryId())
        );

        req.getRequestDispatcher("/WEB-INF/views/customer/products.jsp")
                .forward(req, resp);
    }
}