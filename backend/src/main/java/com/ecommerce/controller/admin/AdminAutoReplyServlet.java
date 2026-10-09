package com.ecommerce.controller.admin;

import com.ecommerce.entity.AutoReply;
import com.ecommerce.service.AdminAutoReplyService;
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

// Quan ly cau tra loi tu dong cua chatbot (Admin). Xem docs/api-spec.md muc 7.
//   GET  /admin/auto-replies            danh sach luat theo thu tu uu tien + o "Thu cau hoi"
//   GET  /admin/auto-replies/test?text= (htmx) luat nao se tra loi cau nay
//   GET  /admin/auto-replies/new        form tao        POST cung URL: tao luat
//   GET  /admin/auto-replies/edit?id=   form sua        POST cung URL: cap nhat
//   POST /admin/auto-replies/toggle     id, active (true|false), back -> bat/tat
//   POST /admin/auto-replies/delete     id, back -> xoa
@WebServlet("/admin/auto-replies/*")
public class AdminAutoReplyServlet extends HttpServlet {

    private static final String LIST_URL = "/admin/auto-replies";

    private final AdminAutoReplyService service = new AdminAutoReplyService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403
        String path = req.getPathInfo() == null ? "" : req.getPathInfo(); // phan URL sau "/admin/auto-replies"

        switch (path) {
            case "", "/" -> {
                req.setAttribute("rules", service.list());
                AdminView.render(req, resp, "admin-autoreply-list.jsp", "Trả lời tự động", "autoreplies");
            }
            case "/test" -> {
                // Mo phong: chatbot se tra loi cau nay the nao. Chi tra manh nho, htmx nhet vao o ket qua.
                String text = ParamUtil.trimOrNull(req.getParameter("text"));
                req.setAttribute("tested", text != null);
                req.setAttribute("matched", text == null ? null : service.test(text));
                req.getRequestDispatcher("/WEB-INF/views/admin/fragments/admin-autoreply-test.jsp").forward(req, resp);
            }
            case "/new" -> {
                AutoReply draft = new AutoReply();
                draft.setPriority(service.suggestPriority()); // goi y: sau cac luat hien co
                req.setAttribute("draft", draft);
                AdminView.render(req, resp, "admin-autoreply-form.jsp", "Thêm câu trả lời tự động", "autoreplies");
            }
            case "/edit" -> {
                try {
                    req.setAttribute("draft", service.getById(ParamUtil.intOrNull(req.getParameter("id"))));
                } catch (BusinessException e) { // id thieu/sai/khong ton tai -> ve danh sach kem thong bao
                    AdminView.flash(req, e.getMessage(), "error");
                    resp.sendRedirect(req.getContextPath() + LIST_URL);
                    return;
                }
                AdminView.render(req, resp, "admin-autoreply-form.jsp", "Sửa câu trả lời tự động", "autoreplies");
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
            case "/toggle" -> {
                try {
                    String message = service.setActive(ParamUtil.intOrNull(req.getParameter("id")), "true".equals(req.getParameter("active")));
                    AdminView.flash(req, message, "success");
                } catch (BusinessException e) {
                    AdminView.flash(req, e.getMessage(), "error");
                }
                resp.sendRedirect(req.getContextPath() + safeBack(req.getParameter("back")));
            }
            case "/delete" -> {
                try {
                    service.delete(ParamUtil.intOrNull(req.getParameter("id")));
                    AdminView.flash(req, "Đã xoá câu trả lời tự động", "success");
                } catch (BusinessException e) {
                    AdminView.flash(req, e.getMessage(), "error");
                }
                resp.sendRedirect(req.getContextPath() + safeBack(req.getParameter("back")));
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // POST tao/sua: doc cac o cua form -> nho Service kiem tra + luu -> thanh cong thi redirect, loi thi ve lai form kem loi
    private void save(HttpServletRequest req, HttpServletResponse resp, Integer id) throws ServletException, IOException {
        String keywords = req.getParameter("keywords");
        String replyText = req.getParameter("replyText");
        String priority = req.getParameter("priority");
        boolean handoff = req.getParameter("handoff") != null; // checkbox chi gui khi duoc tick
        boolean active = req.getParameter("active") != null;
        try {
            service.save(id, keywords, replyText, priority, handoff, active);
            AdminView.flash(req, id == null ? "Đã thêm câu trả lời tự động" : "Đã cập nhật câu trả lời tự động", "success");
            resp.sendRedirect(req.getContextPath() + LIST_URL); // POST-Redirect-GET: F5 khong gui lai form
        } catch (BusinessException e) {
            // Loi validate: ve lai form, giu nguyen cac gia tri vua nhap (draft chi de HIEN THI lai, khong luu DB) va bao loi duoi tung o.
            AutoReply draft = new AutoReply();
            draft.setId(id);
            draft.setKeywords(keywords);
            draft.setReplyText(replyText);
            draft.setPriority(ParamUtil.intOr(priority, 0));
            draft.setHandoff(handoff);
            draft.setActive(active);
            req.setAttribute("draft", draft);
            req.setAttribute("errors", e.getErrors()); // Map<ten o, loi> -> JSP in loi duoi tung o
            if (e.getErrors().isEmpty()) req.setAttribute("formError", e.getMessage()); // loi chung khong thuoc o nao
            AdminView.render(req, resp, "admin-autoreply-form.jsp", id == null ? "Thêm câu trả lời tự động" : "Sửa câu trả lời tự động", "autoreplies");
        }
    }

    // Chi cho quay ve trang NOI BO trong khu nay (bat dau bang "/admin/auto-replies"); chan "//evil.com", "https://evil.com" (open redirect).
    private static String safeBack(String back) {
        boolean ok = back != null && back.startsWith(LIST_URL) && !back.contains("\\") && !back.contains("\r") && !back.contains("\n");
        return ok ? back : LIST_URL;
    }
}
