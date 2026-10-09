package com.ecommerce.controller.admin;

import com.ecommerce.entity.Category;
import com.ecommerce.service.AdminCategoryService;
import com.ecommerce.service.BusinessException;
import com.ecommerce.util.AdminView;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Quan ly danh muc san pham (Admin). Xem docs/api-spec.md muc 7.
//   GET  /admin/categories            danh sach (kem so san pham moi danh muc)
//   GET  /admin/categories/new        form them        POST cung URL: tao danh muc
//   GET  /admin/categories/edit?id=   form sua         POST cung URL: cap nhat
//   POST /admin/categories/delete     id -> xoa (chan neu danh muc con san pham)
@WebServlet("/admin/categories/*")
public class AdminCategoryServlet extends HttpServlet {

    private static final String LIST_URL = "/admin/categories";

    private final AdminCategoryService categoryService = new AdminCategoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403
        String path = req.getPathInfo() == null ? "" : req.getPathInfo(); // phan URL sau "/admin/categories"

        switch (path) {
            case "", "/" -> {
                req.setAttribute("categories", categoryService.list());
                req.setAttribute("counts", categoryService.productCounts()); // id danh muc -> so san pham
                AdminView.render(req, resp, "admin-category-list.jsp", "Danh mục", "categories");
            }
            case "/new" -> {
                req.setAttribute("draft", new Category());
                AdminView.render(req, resp, "admin-category-form.jsp", "Thêm danh mục", "categories");
            }
            case "/edit" -> {
                try {
                    req.setAttribute("draft", categoryService.getById(ParamUtil.intOrNull(req.getParameter("id"))));
                } catch (BusinessException e) { // id thieu/sai/khong ton tai -> ve danh sach kem thong bao
                    AdminView.flash(req, e.getMessage(), "error");
                    resp.sendRedirect(req.getContextPath() + LIST_URL);
                    return;
                }
                AdminView.render(req, resp, "admin-category-form.jsp", "Sửa danh mục", "categories");
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
            case "/delete" -> {
                try {
                    categoryService.delete(ParamUtil.intOrNull(req.getParameter("id")));
                    AdminView.flash(req, "Đã xoá danh mục", "success");
                } catch (BusinessException e) { // vd danh muc con san pham
                    AdminView.flash(req, e.getMessage(), "error");
                }
                resp.sendRedirect(req.getContextPath() + LIST_URL);
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // POST them/sua: doc cac o cua form -> nho Service kiem tra + luu -> thanh cong thi redirect, loi thi ve lai form kem loi
    private void save(HttpServletRequest req, HttpServletResponse resp, Integer id) throws ServletException, IOException {
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        try {
            categoryService.save(id, name, description);
            AdminView.flash(req, id == null ? "Đã thêm danh mục" : "Đã cập nhật danh mục", "success");
            resp.sendRedirect(req.getContextPath() + LIST_URL); // POST-Redirect-GET: F5 khong gui lai form
        } catch (BusinessException e) {
            // Loi validate: ve lai form, giu nguyen cac gia tri vua nhap (draft chi de HIEN THI lai, khong luu DB) va bao loi duoi tung o.
            Category draft = new Category();
            draft.setId(id);
            draft.setName(name);
            draft.setDescription(description);
            req.setAttribute("draft", draft);
            req.setAttribute("errors", e.getErrors()); // Map<ten o, loi> -> JSP in loi duoi tung o
            if (e.getErrors().isEmpty()) req.setAttribute("formError", e.getMessage()); // loi chung khong thuoc o nao
            AdminView.render(req, resp, "admin-category-form.jsp", id == null ? "Thêm danh mục" : "Sửa danh mục", "categories");
        }
    }
}
