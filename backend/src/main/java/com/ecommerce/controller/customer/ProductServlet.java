package com.ecommerce.controller.customer;

import com.ecommerce.dto.ProductFilter;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import com.ecommerce.util.ParamUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// @WebServlet thay the hoan toan khai bao servlet trong web.xml - khong can sua web.xml
// khi them servlet moi.
// GET /products?cat=&q=&min=&max=&instock=on&sort=new|asc|desc&page=   (xem docs/api-spec.md muc 3)
@WebServlet("/products")
public class ProductServlet extends HttpServlet {

    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1. Doc tham so request -> gom vao DTO (tham so sai dinh dang thi bo qua, khong bao loi)
        ProductFilter filter = new ProductFilter();
        filter.setCategoryId(ParamUtil.intOrNull(req.getParameter("cat")));
        filter.setKeyword(ParamUtil.trimOrNull(req.getParameter("q")));
        filter.setMinPrice(ParamUtil.doubleOrNull(req.getParameter("min")));
        filter.setMaxPrice(ParamUtil.doubleOrNull(req.getParameter("max")));
        filter.setInStockOnly("on".equals(req.getParameter("instock")));
        filter.setSort(req.getParameter("sort"));
        filter.setPage(ParamUtil.intOr(req.getParameter("page"), 1));

        // 2. Goi Service (Servlet khong goi thang DAO)
        // 3. Dua du lieu cho JSP qua request attribute, roi forward
        req.setAttribute("filter", filter);
        req.setAttribute("result", productService.search(filter));
        req.setAttribute("categories", categoryService.getAllCategories());
        req.getRequestDispatcher("/WEB-INF/views/customer/products.jsp").forward(req, resp);
    }
}
