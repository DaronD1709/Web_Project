package com.ecommerce.controller.admin;

import com.ecommerce.entity.DiscountType;
import com.ecommerce.entity.Voucher;
import com.ecommerce.service.AdminVoucherService;
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
import java.time.LocalDate;

// Quan ly voucher (Admin). Xem docs/api-spec.md muc 7.
//   GET  /admin/vouchers?tab=       danh sach theo trang thai (all | active | soldout | expired | off)
//   GET  /admin/vouchers/new        form tao        POST cung URL: tao voucher
//   GET  /admin/vouchers/edit?id=   form sua        POST cung URL: cap nhat
//   POST /admin/vouchers/toggle     id, active (true|false), back -> bat/tat
//   POST /admin/vouchers/delete     id, back -> xoa (chan neu da co don dung)
@WebServlet("/admin/vouchers/*")
public class AdminVoucherServlet extends HttpServlet {

    private static final String LIST_URL = "/admin/vouchers";
    private static final String[][] TABS = {
            {"all", "Tất cả"}, {"active", "Đang hoạt động"}, {"soldout", "Hết lượt"}, {"expired", "Hết hạn"}, {"off", "Đã tắt"}};

    private final AdminVoucherService voucherService = new AdminVoucherService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403
        String path = req.getPathInfo() == null ? "" : req.getPathInfo(); // phan URL sau "/admin/vouchers"

        switch (path) {
            case "", "/" -> showList(req, resp);
            case "/new" -> {
                req.setAttribute("draft", newVoucher()); // form tao: mac dinh bat, hieu luc tu hom nay 30 ngay
                showForm(req, resp, "Tạo voucher");
            }
            case "/edit" -> {
                try {
                    req.setAttribute("draft", voucherService.getById(ParamUtil.intOrNull(req.getParameter("id"))));
                } catch (BusinessException e) { // id thieu/sai/khong ton tai -> ve danh sach kem thong bao
                    AdminView.flash(req, e.getMessage(), "error");
                    resp.sendRedirect(req.getContextPath() + LIST_URL);
                    return;
                }
                showForm(req, resp, "Sửa voucher");
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return;
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        switch (path) {
            // /new va /edit dung chung save(): id == null la tao moi, co id la cap nhat
            case "/new", "/edit" -> save(req, resp, "/edit".equals(path) ? ParamUtil.intOrNull(req.getParameter("id")) : null);
            case "/toggle" -> toggle(req, resp);
            case "/delete" -> delete(req, resp);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------ danh sach

    private void showList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String tab = req.getParameter("tab");
        boolean valid = false;
        for (String[] t : TABS) valid |= t[0].equals(tab);
        if (!valid) tab = "all"; // tab la/sai -> hien tat ca

        req.setAttribute("tab", tab);
        req.setAttribute("tabs", TABS);
        req.setAttribute("tabCounts", voucherService.tabCounts());
        req.setAttribute("vouchers", voucherService.list(tab));
        AdminView.render(req, resp, "admin-voucher-list.jsp", "Voucher", "vouchers");
    }

    // ------------------------------------------------------------------ form tao / sua

    private void showForm(HttpServletRequest req, HttpServletResponse resp, String title) throws ServletException, IOException {
        AdminView.render(req, resp, "admin-voucher-form.jsp", title, "vouchers");
    }

    private static Voucher newVoucher() {
        Voucher v = new Voucher();
        v.setDiscountType(DiscountType.PERCENTAGE);
        v.setQuantityIssued(100);
        v.setStartDate(LocalDate.now().atStartOfDay());
        v.setEndDate(LocalDate.now().plusDays(30).atStartOfDay());
        v.setActive(true);
        return v;
    }

    // POST tao/sua: doc cac o cua form -> nho Service kiem tra + luu -> thanh cong thi redirect, loi thi ve lai form kem loi
    private void save(HttpServletRequest req, HttpServletResponse resp, Integer id) throws ServletException, IOException {
        String code = req.getParameter("code");
        String type = req.getParameter("type");
        String value = req.getParameter("value");
        String min = req.getParameter("min");
        String issued = req.getParameter("issued");
        String start = req.getParameter("start");
        String end = req.getParameter("end");
        boolean active = req.getParameter("active") != null; // checkbox chi gui khi duoc tick
        try {
            voucherService.save(id, code, type, value, min, issued, start, end, active);
            AdminView.flash(req, id == null ? "Đã tạo voucher" : "Đã cập nhật voucher", "success");
            resp.sendRedirect(req.getContextPath() + LIST_URL); // POST-Redirect-GET: F5 khong gui lai form
        } catch (BusinessException e) {
            // Loi validate: ve lai form, giu nguyen cac gia tri vua nhap (draft chi de HIEN THI lai, khong luu DB) va bao loi duoi tung o.
            Voucher draft = new Voucher();
            draft.setId(id);
            draft.setCode(code);
            draft.setDiscountType("fixed".equals(type) ? DiscountType.FIXED_AMOUNT : DiscountType.PERCENTAGE);
            Double v = ParamUtil.doubleOrNull(value);
            draft.setDiscountValue(v == null ? 0 : v);
            Double m = ParamUtil.doubleOrNull(min);
            draft.setMinOrderValue(m == null ? 0 : m);
            Integer q = ParamUtil.intOrNull(issued);
            draft.setQuantityIssued(q == null ? 0 : q);
            LocalDate s = ParamUtil.dateOrNull(start), en = ParamUtil.dateOrNull(end);
            draft.setStartDate(s == null ? null : s.atStartOfDay());
            draft.setEndDate(en == null ? null : en.atStartOfDay());
            draft.setActive(active);
            req.setAttribute("draft", draft);
            req.setAttribute("errors", e.getErrors()); // Map<ten o, loi> -> JSP in loi duoi tung o
            if (e.getErrors().isEmpty()) req.setAttribute("formError", e.getMessage()); // loi chung khong thuoc o nao
            showForm(req, resp, id == null ? "Tạo voucher" : "Sửa voucher");
        }
    }

    // ------------------------------------------------------------------ bat/tat, xoa

    // Bat/tat: form nho tren moi dong (htmx tu gui khi gat cong tac). Xong redirect ve "back" (cung tab) kem toast.
    private void toggle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String message = voucherService.setActive(ParamUtil.intOrNull(req.getParameter("id")), "true".equals(req.getParameter("active")));
            AdminView.flash(req, message, "success");
        } catch (BusinessException e) {
            AdminView.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + safeBack(req.getParameter("back")));
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            voucherService.delete(ParamUtil.intOrNull(req.getParameter("id")));
            AdminView.flash(req, "Đã xoá voucher", "success");
        } catch (BusinessException e) { // vd voucher da co don dung
            AdminView.flash(req, e.getMessage(), "error");
        }
        resp.sendRedirect(req.getContextPath() + safeBack(req.getParameter("back")));
    }

    // Chi cho quay ve trang NOI BO trong khu voucher (bat dau bang "/admin/vouchers"); chan "//evil.com", "https://evil.com" (open redirect).
    private static String safeBack(String back) {
        boolean ok = back != null && back.startsWith(LIST_URL) && !back.contains("\\") && !back.contains("\r") && !back.contains("\n");
        return ok ? back : LIST_URL;
    }
}
