package com.ecommerce.controller.admin;

import com.ecommerce.entity.Admin;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Message;
import com.ecommerce.service.AdminChatService;
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
import java.util.List;

// Chat voi khach (Admin). Khong dung WebSocket: trinh duyet tu hoi server dinh ky bang htmx (xem docs/api-spec.md muc 7).
//   GET  /admin/chat?c=&q=           trang chat: danh sach hoi thoai + khung chat + thong tin khach (c = khach dang mo, mac dinh hoi thoai moi nhat)
//   GET  /admin/chat/list            danh sach hoi thoai (htmx polling moi 5 giay, hoac khi go o tim kiem)
//   GET  /admin/chat/messages        tin moi cua 1 khach (htmx polling moi 3 giay): c, after
//   POST /admin/chat/send            c, content, after -> gui tin tra loi; tra ve cac tin moi (gom ca tin vua gui)
//
// CACH HOAT DONG (doc phan nay de giai thich khi van dap):
//   1. Admin mo /admin/chat -> showPage dung CA TRANG (admin-chat.jsp): cot trai = danh sach khach, giua = khung chat, phai = thong tin khach.
//   2. Cot trai tu hoi /admin/chat/list moi 5 giay; khung chat tu hoi /admin/chat/messages?after=<id tin cuoi> moi 3 giay.
//      Moi lan hoi, server chi tra cac tin CO ID LON HON "after" (xem MessageDAO.findAfter) nen rat nhe.
//   3. Admin go tin -> POST /admin/chat/send -> luu tin, tra ve cac tin moi (gom ca tin vua gui). Khach thay tin o lan polling ke tiep.
// Khong co chatbot: shop tra loi bang tay. Servlet chi dieu phoi; nghiep vu o AdminChatService, du lieu o MessageDAO.
@WebServlet("/admin/chat/*")
public class AdminChatServlet extends HttpServlet {

    // Thu muc chua cac manh JSP (fragment) tra ve cho htmx: admin-chat-list / admin-chat-messages.
    private static final String VIEWS = "/WEB-INF/views/admin/fragments/";

    private final AdminChatService chatService = new AdminChatService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403
        String path = req.getPathInfo() == null ? "" : req.getPathInfo(); // phan URL sau "/admin/chat"

        switch (path) {
            case "", "/" -> showPage(req, resp);
            case "/list" -> {
                // Chi tra manh danh sach hoi thoai (khong phai ca trang) de htmx thay vao cot trai khi polling hoac khi go o tim kiem.
                String q = ParamUtil.trimOrNull(req.getParameter("q"));
                req.setAttribute("conversations", chatService.conversations(q));
                req.setAttribute("selectedId", ParamUtil.intOrNull(req.getParameter("c")));
                req.setAttribute("q", q);
                req.getRequestDispatcher(VIEWS + "admin-chat-list.jsp").forward(req, resp);
            }
            // Polling khung chat: after = id tin cuoi ma trinh duyet da hien; chi tra cac tin moi hon.
            case "/messages" -> forwardMessages(req, resp, ParamUtil.intOrNull(req.getParameter("c")),
                    ParamUtil.intOr(req.getParameter("after"), 0));
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Admin admin = SessionUtil.requireAdmin(req, resp);
        if (admin == null) return;
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        Integer customerId = ParamUtil.intOrNull(req.getParameter("c"));

        if (!"/send".equals(path)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            chatService.send(admin, customerId, req.getParameter("content"));
        } catch (BusinessException e) {
            // Loi nhap lieu (tin rong/qua dai): bao bang toast; "HX-Reswap: none" bao htmx KHONG thay noi dung nao,
            // nen khung chat va o dang go van nguyen, Admin khong bi mat chu.
            HtmxUtil.toast(resp, e.getMessage(), "error");
            resp.setHeader("HX-Reswap", "none"); // giu nguyen khung chat, chi hien thong bao loi
            return;
        }
        // Tra tin moi tu luc form biet (after) -> gom ca tin vua gui
        forwardMessages(req, resp, customerId, ParamUtil.intOr(req.getParameter("after"), 0));
    }

    // ------------------------------------------------------------------ trang chat

    // GET /admin/chat: dung trang chat day du. Cac attribute duoi day la du lieu cho admin-chat.jsp:
    //   conversations (cot trai) · selected/selectedId (khach dang mo) · messages + lastId (khung chat, lastId de polling) ·
    //   orders (3 don gan nhat o panel phai) · q (giu lai o tim kiem).
    private void showPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String q = ParamUtil.trimOrNull(req.getParameter("q"));
        List<Message> conversations = chatService.conversations(q);

        // Khach dang mo: theo ?c=, khong co (hoac khong hop le) thi lay hoi thoai moi nhat trong danh sach
        Customer selected = null;
        Integer wanted = ParamUtil.intOrNull(req.getParameter("c"));
        if (wanted != null) {
            try {
                selected = chatService.getCustomer(wanted);
            } catch (BusinessException ignored) {
                // id la -> roi ve hoi thoai moi nhat
            }
        }
        if (selected == null && !conversations.isEmpty()) selected = conversations.get(0).getCustomer();

        req.setAttribute("conversations", conversations);
        req.setAttribute("selectedId", selected == null ? null : selected.getId());
        req.setAttribute("selected", selected);
        req.setAttribute("q", q);
        if (selected != null) {
            List<Message> messages = chatService.messagesAfter(selected.getId(), 0);
            req.setAttribute("messages", messages);
            req.setAttribute("lastId", messages.isEmpty() ? 0 : messages.get(messages.size() - 1).getId());
            req.setAttribute("orders", chatService.recentOrders(selected.getId()));
        }
        AdminView.render(req, resp, "admin-chat.jsp", "Chat với khách", "chat");
    }

    // Tin moi cua 1 khach (sau `after`) + the #poll hoi tiep.
    // lastId la id tin cuoi vua tra: fragment dat no vao URL cua the #poll de lan polling sau chi xin tin moi hon (khong gui trung tin).
    private void forwardMessages(HttpServletRequest req, HttpServletResponse resp, Integer customerId, int after)
            throws ServletException, IOException {
        Customer customer;
        try {
            customer = chatService.getCustomer(customerId);
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<Message> messages = chatService.messagesAfter(customerId, after);
        req.setAttribute("selected", customer);
        req.setAttribute("messages", messages);
        req.setAttribute("lastId", messages.isEmpty() ? after : messages.get(messages.size() - 1).getId());
        req.getRequestDispatcher(VIEWS + "admin-chat-messages.jsp").forward(req, resp);
    }
}
