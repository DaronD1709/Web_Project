package com.ecommerce.controller.admin;

import com.ecommerce.dto.AdminCustomerFilter;
import com.ecommerce.entity.Customer;
import com.ecommerce.service.AdminCustomerService;
import com.ecommerce.service.BusinessException;
import com.ecommerce.util.AdminView;
import com.ecommerce.util.HtmxUtil;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Quan ly tai khoan khach hang (Admin). Xem docs/api-spec.md muc 7.
//   GET  /admin/users            danh sach (q, status, page); htmx tu o loc/phan trang -> chi tra bang (#customer-list)
//   GET  /admin/users/detail?id= ho so + thong ke + don gan day
//   POST /admin/users/lock       id, locked (true|false), back -> khoa / mo khoa
@WebServlet("/admin/users/*")
public class AdminCustomerServlet extends HttpServlet {

    private static final String LIST_URL = "/admin/users";

    private final AdminCustomerService customerService = new AdminCustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403
        String path = req.getPathInfo() == null ? "" : req.getPathInfo(); // phan URL sau "/admin/users"

        switch (path) {
            case "", "/" -> showList(req, resp);
            case "/detail" -> showDetail(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return;
        if (!"/lock".equals(req.getPathInfo())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        lock(req, resp);
    }

    // GET /admin/users: doc bo loc -> nho Service tim -> tra CA TRANG hay chi BANG
    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AdminCustomerFilter filter = new AdminCustomerFilter();
        filter.setKeyword(ParamUtil.trimOrNull(req.getParameter("q")));
        filter.setStatus(req.getParameter("status"));
        filter.setPage(ParamUtil.intOr(req.getParameter("page"), 1));

        var result = customerService.search(filter);
        req.setAttribute("filter", filter);
        req.setAttribute("result", result);
        req.setAttribute("stats", customerService.stats(result.getItems())); // id khach -> {so don, tong chi tieu}
        req.setAttribute("counts", customerService.counts());                  // tong khach / so khach bi khoa

        // O loc / phan trang (htmx) gui HX-Target = customer-list nen chi can bang; mo trang hoac chuyen trang tu sidebar can ca noi dung.
        if (HtmxUtil.isHtmx(req) && "customer-list".equals(req.getHeader("HX-Target"))) {
            resp.addHeader("Vary", "HX-Request, HX-Target");
            req.getRequestDispatcher("/WEB-INF/views/admin/fragments/admin-customer-table.jsp").forward(req, resp);
        } else {
            AdminView.render(req, resp, "admin-customer-list.jsp", "Khách hàng", "customers");
        }
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Customer customer = customerService.getById(ParamUtil.intOrNull(req.getParameter("id")));
            req.setAttribute("customer", customer);
            req.setAttribute("stat", customerService.stats(java.util.List.of(customer)).get(customer.getId())); // null neu chua co don
            req.setAttribute("orders", customerService.recentOrders(customer.getId()));
        } catch (BusinessException e) { // id thieu/sai/khong ton tai -> ve danh sach kem thong bao
            AdminView.flash(req, e.getMessage(), "error");
            resp.sendRedirect(req.getContextPath() + LIST_URL);
            return;
        }
        AdminView.render(req, resp, "admin-customer-detail.jsp", "Chi tiết khách hàng", "customers");
    }

    // POST /admin/users/lock: Service khoa/mo khoa; xong redirect ve "back" (danh sach dung bo loc hoac trang chi tiet) kem thong bao flash
    private void lock(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String message = customerService.setLocked(ParamUtil.intOrNull(req.getParameter("id")), "true".equals(req.getParameter("locked")));
            AdminView.flash(req, message, "success");
        } catch (BusinessException e) {
            AdminView.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + safeBack(req.getParameter("back")));
    }

    // Chi cho quay ve trang NOI BO trong khu khach hang (bat dau bang "/admin/users"); chan "//evil.com", "https://evil.com" (open redirect).
    private static String safeBack(String back) {
        boolean ok = back != null && back.startsWith(LIST_URL) && !back.contains("\\") && !back.contains("\r") && !back.contains("\n");
        return ok ? back : LIST_URL;
    }
}
