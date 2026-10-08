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

    private static final int PAGE_SIZE = 10; // so san pham moi trang trong bang admin

    private final ProductService productService = new ProductService();             // doc / tim kiem (dung chung voi khach hang)
    private final AdminProductService adminProductService = new AdminProductService(); // them / sua / xoa (chi Admin)
    private final CategoryService categoryService = new CategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Chua dang nhap -> chuyen toi /login; la khach hang -> 403. requireAdmin da tra loi xong thi chi viec return.
        if (SessionUtil.requireAdmin(req, resp) == null) return;
        // pathInfo la phan URL sau "/admin/products": "" (danh sach), "/new", "/edit"... -> dung de chon viec can lam
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        switch (path) {
            case "", "/" -> showList(req, resp);
            case "/new" -> {
                req.setAttribute("draft", new Product()); // form them: san pham rong
                showForm(req, resp, "Thêm sản phẩm");
            }
            case "/edit" -> {
                Integer id = ParamUtil.intOrNull(req.getParameter("id"));
                Product product = id == null ? null : productService.getProductById(id);
                if (product == null) { // id thieu/sai/khong ton tai -> ve danh sach kem thong bao loi
                    AdminView.flash(req, "Sản phẩm không tồn tại.", "error");
                    resp.sendRedirect(req.getContextPath() + "/admin/products");
                    return;
                }
                req.setAttribute("draft", product); // form sua: dien san du lieu cua san pham
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
            // /new va /edit dung chung save(): id == null la them moi, co id la cap nhat
            case "/new", "/edit" -> save(req, resp, "/edit".equals(path) ? ParamUtil.intOrNull(req.getParameter("id")) : null);
            case "/delete" -> delete(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------ danh sach

    // GET /admin/products: doc bo loc tu URL -> nho Service tim -> chon tra CA TRANG hay chi BANG
    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ProductFilter filter = readFilter(req);
        req.setAttribute("filter", filter);
        req.setAttribute("result", productService.search(filter));
        req.setAttribute("categories", categoryService.getAllCategories());

        // O loc / phan trang (htmx) chi can bang san pham; mo trang / chuyen trang tu sidebar can ca noi dung trang.
        // htmx tu gui header HX-Target = id cua the dich (hx-target="#product-list") nen dua vao do phan biet.
        if (HtmxUtil.isHtmx(req) && "product-list".equals(req.getHeader("HX-Target"))) {
            resp.addHeader("Vary", "HX-Request, HX-Target");
            req.getRequestDispatcher("/WEB-INF/views/admin/fragments/admin-product-table.jsp").forward(req, resp);
        } else {
            AdminView.render(req, resp, "admin-product-list.jsp", "Sản phẩm", "products");
        }
    }

    // Gom tham so URL (?q=&cat=&stock=&sort=&page=) vao ProductFilter; gia tri sai thi bo qua, khong bao loi (ParamUtil)
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

    // Ve form them/sua (admin-product-form.jsp). Caller da dat san attribute "draft" (san pham dang hien tren form).
    private void showForm(HttpServletRequest req, HttpServletResponse resp, String title) throws ServletException, IOException {
        req.setAttribute("categories", categoryService.getAllCategories());
        AdminView.render(req, resp, "admin-product-form.jsp", title, "products");
    }

    // POST them/sua: doc cac o cua form -> nho Service kiem tra + luu -> thanh cong thi redirect, loi thi ve lai form
    private void save(HttpServletRequest req, HttpServletResponse resp, Integer id) throws ServletException, IOException {
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        String price = req.getParameter("price");
        String stock = req.getParameter("stock");
        Integer categoryId = ParamUtil.intOrNull(req.getParameter("category"));
        try {
            Part imagePart = req.getPart("image"); // file tai len (@MultipartConfig); khong chon file thi size = 0 -> image = null
            byte[] image = (imagePart == null || imagePart.getSize() == 0) ? null : imagePart.getInputStream().readAllBytes();
            adminProductService.save(id, name, description, price, stock, categoryId, image, req.getParameter("removeImage") != null);

            AdminView.flash(req, id == null ? "Đã thêm sản phẩm" : "Đã cập nhật sản phẩm", "success");
            resp.sendRedirect(req.getContextPath() + "/admin/products"); // POST-Redirect-GET: F5 khong gui lai form
        } catch (BusinessException e) {
            // Loi validate: ve lai form, giu nguyen cac gia tri vua nhap (draft) va bao loi ngay duoi tung o.
            // draft chi de HIEN THI lai (khong luu DB): dung gia tri nguoi dung vua go, anh van la anh dang co cua san pham.
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
            req.setAttribute("errors", e.getErrors()); // Map<ten o, loi> -> JSP in loi duoi tung o
            if (e.getErrors().isEmpty()) req.setAttribute("formError", e.getMessage()); // loi chung khong thuoc o nao
            showForm(req, resp, id == null ? "Thêm sản phẩm" : "Sửa sản phẩm");
        }
    }

    // ------------------------------------------------------------------ xoa

    // POST /admin/products/delete. Co 2 buoc: (1) Service xoa (co the bi tu choi), (2) tra loi tuy noi bam nut Xoa.
    private void delete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer id = ParamUtil.intOrNull(req.getParameter("id"));
        try {
            if (id == null) throw new BusinessException("Thiếu mã sản phẩm.");
            adminProductService.delete(id);
        } catch (BusinessException e) { // vd san pham da co trong don hang
            if (HtmxUtil.isHtmx(req)) {
                HtmxUtil.toast(resp, e.getMessage(), "error");
                resp.setHeader("HX-Reswap", "none"); // giu nguyen dong trong bang, chi hien thong bao
            } else {
                AdminView.flash(req, e.getMessage(), "error");
                resp.sendRedirect(req.getContextPath() + "/admin/products");
            }
            return;
        }

        // Xoa thanh cong. 3 kieu tra loi:
        if (!HtmxUtil.isHtmx(req)) { // (a) khong phai htmx: redirect ve danh sach
            AdminView.flash(req, "Đã xoá sản phẩm", "success");
            resp.sendRedirect(req.getContextPath() + "/admin/products");
        } else if (req.getParameter("redirect") != null) { // (b) nut Xoa trong trang form: HX-Redirect ve danh sach
            AdminView.flash(req, "Đã xoá sản phẩm", "success");
            HtmxUtil.redirect(resp, req.getContextPath() + "/admin/products");
        } else { // (c) nut Xoa trong bang: tra manh rong (dong bien mat) + cap nhat dong "Hien thi a-b / tong" bang hx-swap-oob
            HtmxUtil.toast(resp, "Đã xoá sản phẩm", "success");
            ProductFilter filter = readFilter(req);
            req.setAttribute("filter", filter);
            req.setAttribute("result", productService.search(filter));
            req.getRequestDispatcher("/WEB-INF/views/admin/fragments/admin-product-deleted.jsp").forward(req, resp);
        }
    }
}
