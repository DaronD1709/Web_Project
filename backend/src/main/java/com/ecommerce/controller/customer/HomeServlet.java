package com.ecommerce.controller.customer;

import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Trang chu cua khach: GET / (goc web) va GET /home  (xem docs/api-spec.md muc 3)
@WebServlet(urlPatterns = {"", "/home"})
public class HomeServlet extends HttpServlet {

    private static final int FEATURED_COUNT = 8;
    private final ProductService productService = new ProductService();
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("categories", categoryService.getAllCategories());
        req.setAttribute("featuredProducts", productService.getLatestProducts(FEATURED_COUNT));
        req.getRequestDispatcher("/WEB-INF/views/customer/home.jsp").forward(req, resp);
    }
}
