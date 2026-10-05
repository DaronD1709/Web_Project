package com.ecommerce.controller.admin;

import com.ecommerce.dto.ProductFilter;
import com.ecommerce.entity.Admin;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.service.AdminProductService;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.CategoryService;
import com.ecommerce.service.ProductService;
import com.ecommerce.util.AdminView;
import com.ecommerce.util.HtmxUtil;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;

// Quan ly san pham (Admin). Xem docs/api-spec.md muc 7.
//   GET  /admin/products            danh sach (q, cat, stock, sort, page); htmx tu o loc -> chi tra bang (#product-list)
//   GET  /admin/products/new        form them          POST cung URL: tao san pham (multipart, co anh)
//   GET  /admin/products/edit?id=   form sua           POST cung URL: cap nhat
//   POST /admin/products/delete     xoa (htmx): bo dong khoi bang + cap nhat dong tong so
// maxFileSize chi la tran bao ve bo nho; gioi han 2MB cua anh do ProductService kiem tra de khi qua lon van giu duoc du lieu form.
@WebServlet("/admin/products/*")
@MultipartConfig(maxFileSize = 6 * 1024 * 1024, maxRequestSize = 7 * 1024 * 1024)
public class AdminProductServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;

    private final ProductService productService = new ProductService();             // doc / tim kiem (dung chung voi khach hang)
    private final AdminProductService adminProductService = new AdminProductService(); // them / sua / xoa (chi Admin)
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return;
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        switch (path) {
            case "", "/" -> showList(req, resp);
            case "/new" -> {
                req.setAttribute("draft", new Product());
                showForm(req, resp, "Thêm sản phẩm");
            }
            case "/edit" -> {
                Integer id = ParamUtil.intOrNull(req.getParameter("id"));
                Product product = id == null ? null : productService.getProductById(id);
                if (product == null) {
                    AdminView.flash(req, "Sản phẩm không tồn tại.", "error");
                    resp.sendRedirect(req.getContextPath() + "/admin/products");
                    return;
                }
                req.setAttribute("draft", product);
                showForm(req, resp, "Sửa sản phẩm");
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return;
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        switch (path) {
            case "/new", "/edit" -> save(req, resp, "/edit".equals(path) ? ParamUtil.intOrNull(req.getParameter("id")) : null);
            case "/delete" -> delete(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------ danh sach

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ProductFilter filter = readFilter(req);
        req.setAttribute("filter", filter);
        req.setAttribute("result", productService.search(filter));
        req.setAttribute("categories", categoryService.getAllCategories());

        // O loc / phan trang (htmx) chi can bang san pham; mo trang / chuyen trang tu sidebar can ca noi dung trang.
        if (HtmxUtil.isHtmx(req) && "product-list".equals(req.getHeader("HX-Target"))) {
            resp.addHeader("Vary", "HX-Request, HX-Target");
            req.getRequestDispatcher("/WEB-INF/views/admin/fragments/admin-product-table.jsp").forward(req, resp);
        } else {
            AdminView.render(req, resp, "admin-product-list.jsp", "Sản phẩm", "products");
        }
    }

    private ProductFilter readFilter(HttpServletRequest req) {
        ProductFilter filter = new ProductFilter();
        filter.setCategoryId(ParamUtil.intOrNull(req.getParameter("cat")));
        filter.setKeyword(ParamUtil.trimOrNull(req.getParameter("q")));
        filter.setStock(req.getParameter("stock"));
        filter.setSort(req.getParameter("sort"));
        filter.setPage(ParamUtil.intOr(req.getParameter("page"), 1));
        filter.setPageSize(PAGE_SIZE);
        return filter;
    }

    // ------------------------------------------------------------------ form them / sua

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String title) throws ServletException, IOException {
        req.setAttribute("categories", categoryService.getAllCategories());
        AdminView.render(req, resp, "admin-product-form.jsp", title, "products");
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, Integer id) throws ServletException, IOException {
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String price = req.getParameter("price");
        String stock = req.getParameter("stock");
        Integer categoryId = ParamUtil.intOrNull(req.getParameter("category"));
        try {
            Part imagePart = req.getPart("image");
            byte[] image = (imagePart == null || imagePart.getSize() == 0) ? null : imagePart.getInputStream().readAllBytes();
            adminProductService.save(id, name, description, price, stock, categoryId, image, req.getParameter("removeImage") != null);

            AdminView.flash(req, id == null ? "Đã thêm sản phẩm" : "Đã cập nhật sản phẩm", "success");
            resp.sendRedirect(req.getContextPath() + "/admin/products"); // POST-Redirect-GET: F5 khong gui lai form
        } catch (BusinessException e) {
            // Loi validate: ve lai form, giu nguyen cac gia tri vua nhap (draft) va bao loi ngay duoi tung o.
            Product draft = new Product();
            if (id != null) {
                Product current = productService.getProductById(id);
                if (current != null) draft.setImageUrl(current.getImageUrl());
            }
            draft.setId(id);
            draft.setName(name);
            draft.setDescription(description);
            Double priceValue = ParamUtil.doubleOrNull(price);
            draft.setPrice(priceValue == null ? 0 : priceValue);
            Integer stockValue = ParamUtil.intOrNull(stock);
            draft.setStockQuantity(stockValue == null ? 0 : stockValue);
            if (categoryId != null) {
                Category category = new Category();
                category.setId(categoryId);
                draft.setCategory(category);
            }
            req.setAttribute("draft", draft);
            req.setAttribute("errors", e.getErrors());
            if (e.getErrors().isEmpty()) req.setAttribute("formError", e.getMessage());
            showForm(req, resp, id == null ? "Thêm sản phẩm" : "Sửa sản phẩm");
        }
    }

    // ------------------------------------------------------------------ xoa

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer id = ParamUtil.intOrNull(req.getParameter("id"));
        try {
            if (id == null) throw new BusinessException("Thiếu mã sản phẩm.");
            adminProductService.delete(id);
        } catch (BusinessException e) {
            if (HtmxUtil.isHtmx(req)) {
                HtmxUtil.toast(resp, e.getMessage(), "error");
                resp.setHeader("HX-Reswap", "none"); // giu nguyen dong trong bang, chi hien thong bao
            } else {
                AdminView.flash(req, e.getMessage(), "error");
                resp.sendRedirect(req.getContextPath() + "/admin/products");
            }
            return;
        }

        if (!HtmxUtil.isHtmx(req)) {
            AdminView.flash(req, "Đã xoá sản phẩm", "success");
            resp.sendRedirect(req.getContextPath() + "/admin/products");
        } else if (req.getParameter("redirect") != null) { // nut Xoa trong trang form: quay ve danh sach
            AdminView.flash(req, "Đã xoá sản phẩm", "success");
            HtmxUtil.redirect(resp, req.getContextPath() + "/admin/products");
        } else { // nut Xoa trong bang: tra manh rong (dong bien mat) + cap nhat dong "Hien thi a-b / tong" bang hx-swap-oob
            HtmxUtil.toast(resp, "Đã xoá sản phẩm", "success");
            ProductFilter filter = readFilter(req);
            req.setAttribute("filter", filter);
            req.setAttribute("result", productService.search(filter));
            req.getRequestDispatcher("/WEB-INF/views/admin/fragments/admin-product-deleted.jsp").forward(req, resp);
        }
    }
}
