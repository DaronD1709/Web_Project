package com.ecommerce.controller.admin;

import com.ecommerce.dto.AdminOrderFilter;
import com.ecommerce.service.AdminOrderService;
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

// Quan ly don hang (Admin). Xem docs/api-spec.md muc 7.
//   GET  /admin/orders            danh sach (status, q, pay, from, to, page); htmx tu o loc/tab -> chi tra bang (#order-list)
//   GET  /admin/orders/detail?id= chi tiet 1 don
//   POST /admin/orders/status     id, status (ten OrderStatus), back -> doi trang thai (xac nhan/giao/hoan tat/huy/duyet hoac tu choi hoan hang)
@WebServlet("/admin/orders/*")
public class AdminOrderServlet extends HttpServlet {

    private static final String LIST_URL = "/admin/orders";

    // Cac tab tren danh sach: {khoa gui len server, nhan hien thi}
    private static final String[][] TABS = {
            {"all", "Tất cả"}, {"pending", "Chờ xác nhận"}, {"confirmed", "Đã xác nhận"}, {"shipping", "Đang giao"},
            {"completed", "Hoàn tất"}, {"cancelled", "Đã huỷ"}, {"return", "Hoàn hàng"}};

    private final AdminOrderService orderService = new AdminOrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403
        String path = req.getPathInfo() == null ? "" : req.getPathInfo(); // phan URL sau "/admin/orders"

        switch (path) {
            case "", "/" -> showList(req, resp);
            case "/detail" -> showDetail(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return;
        if (!"/status".equals(req.getPathInfo())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        changeStatus(req, resp);
    }

    // ------------------------------------------------------------------ danh sach

    // GET /admin/orders: doc bo loc -> nho Service tim -> tra CA TRANG hay chi BANG
    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AdminOrderFilter filter = new AdminOrderFilter();
        filter.setStatus(req.getParameter("status"));
        filter.setKeyword(ParamUtil.trimOrNull(req.getParameter("q")));
        filter.setPay(req.getParameter("pay"));
        filter.setFrom(ParamUtil.dateOrNull(req.getParameter("from")));
        filter.setTo(ParamUtil.dateOrNull(req.getParameter("to")));
        filter.setPage(ParamUtil.intOr(req.getParameter("page"), 1));

        var result = orderService.search(filter);
        req.setAttribute("filter", filter);
        req.setAttribute("result", result);
        req.setAttribute("itemCounts", orderService.countItems(result.getItems())); // id don -> so mat hang
        req.setAttribute("tabCounts", orderService.tabCounts());                      // so don tren moi tab
        req.setAttribute("tabs", TABS);

        // O loc / tab / phan trang (htmx) gui HX-Target = order-list nen chi can phan bang; mo trang hoac chuyen trang tu sidebar can ca noi dung.
        if (HtmxUtil.isHtmx(req) && "order-list".equals(req.getHeader("HX-Target"))) {
            resp.addHeader("Vary", "HX-Request, HX-Target");
            req.getRequestDispatcher("/WEB-INF/views/admin/fragments/admin-order-table.jsp").forward(req, resp);
        } else {
            AdminView.render(req, resp, "admin-order-list.jsp", "Đơn hàng", "orders");
        }
    }

    // ------------------------------------------------------------------ chi tiet

    private void showDetail(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("order", orderService.getDetail(ParamUtil.intOrNull(req.getParameter("id"))));
        } catch (BusinessException e) { // id thieu/sai/khong ton tai -> ve danh sach kem thong bao
            AdminView.flash(req, e.getMessage(), "error");
            resp.sendRedirect(req.getContextPath() + LIST_URL);
            return;
        }
        AdminView.render(req, resp, "admin-order-detail.jsp", "Chi tiết đơn hàng", "orders");
    }

    // ------------------------------------------------------------------ doi trang thai

    // POST /admin/orders/status: Service kiem tra + ghi; xong redirect ve "back" (danh sach dung bo loc hoac trang chi tiet)
    // kem thong bao flash (thanh cong hoac loi). Dung PRG nen F5 khong gui lai form.
    private void changeStatus(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String message = orderService.changeStatus(ParamUtil.intOrNull(req.getParameter("id")), req.getParameter("status"));
            AdminView.flash(req, message, "success");
        } catch (BusinessException e) {
            AdminView.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + safeBack(req.getParameter("back")));
    }

    // Chi cho quay ve trang NOI BO trong khu don hang (bat dau bang "/admin/orders"); chan "//evil.com", "https://evil.com" (open redirect).
    private static String safeBack(String back) {
        boolean ok = back != null && back.startsWith(LIST_URL) && !back.contains("\\") && !back.contains("\r") && !back.contains("\n");
        return ok ? back : LIST_URL;
    }
}
