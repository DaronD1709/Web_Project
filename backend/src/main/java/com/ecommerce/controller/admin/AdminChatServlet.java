package com.ecommerce.controller.admin;

import com.ecommerce.entity.Admin;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Message;
import com.ecommerce.service.AdminChatService;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.ChatService;
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
//   GET  /admin/chat?c=&filter=&q=   trang chat: danh sach hoi thoai + khung chat + thong tin khach (c = khach dang mo, mac dinh hoi thoai moi nhat)
//   GET  /admin/chat/list            danh sach hoi thoai (htmx polling moi 5 giay, hoac khi go o tim kiem)
//   GET  /admin/chat/messages        tin moi cua 1 khach (htmx polling moi 3 giay): c, after
//   POST /admin/chat/send            c, content, after -> gui tin; tra ve tin moi + dau do che do "nhan vien" cap nhat (hx-swap-oob)
//   POST /admin/chat/mode            c, human (true|false) -> bat/tat che do nhan vien tiep quan
@WebServlet("/admin/chat/*")
public class AdminChatServlet extends HttpServlet {

    private static final String VIEWS = "/WEB-INF/views/admin/fragments/";

    private final AdminChatService chatService = new AdminChatService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403
        String path = req.getPathInfo() == null ? "" : req.getPathInfo(); // phan URL sau "/admin/chat"

        switch (path) {
            case "", "/" -> showPage(req, resp);
            case "/list" -> {
                String filter = filterOf(req), q = ParamUtil.trimOrNull(req.getParameter("q"));
                req.setAttribute("conversations", chatService.conversations(filter, q));
                req.setAttribute("selectedId", ParamUtil.intOrNull(req.getParameter("c")));
                req.setAttribute("filter", filter);
                req.setAttribute("q", q);
                req.getRequestDispatcher(VIEWS + "admin-chat-list.jsp").forward(req, resp);
            }
            case "/messages" -> forwardMessages(req, resp, ParamUtil.intOrNull(req.getParameter("c")),
                    ParamUtil.intOr(req.getParameter("after"), 0), false);
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Admin admin = SessionUtil.requireAdmin(req, resp);
        if (admin == null) return;
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        Integer customerId = ParamUtil.intOrNull(req.getParameter("c"));

        switch (path) {
            case "/send" -> {
                try {
                    chatService.send(admin, customerId, req.getParameter("content"));
                } catch (BusinessException e) {
                    HtmxUtil.toast(resp, e.getMessage(), "error");
                    resp.setHeader("HX-Reswap", "none"); // giu nguyen khung chat, chi hien thong bao loi
                    return;
                }
                // Tra tin moi tu luc form biet (after) -> gom ca tin vua gui; kem dau do che do (da tu chuyen sang "nhan vien")
                forwardMessages(req, resp, customerId, ParamUtil.intOr(req.getParameter("after"), 0), true);
            }
            case "/mode" -> {
                try {
                    String message = chatService.setHuman(customerId, "true".equals(req.getParameter("human")));
                    HtmxUtil.toast(resp, message, "success");
                } catch (BusinessException e) {
                    HtmxUtil.toast(resp, e.getMessage(), "error");
                    resp.setHeader("HX-Reswap", "none");
                    return;
                }
                req.setAttribute("selected", chatService.getCustomer(customerId));
                req.getRequestDispatcher(VIEWS + "admin-chat-head.jsp").forward(req, resp); // thay chinh #chat-head
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------ trang chat

    private void showPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String filter = filterOf(req), q = ParamUtil.trimOrNull(req.getParameter("q"));
        List<Message> conversations = chatService.conversations(filter, q);

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
        req.setAttribute("filter", filter);
        req.setAttribute("q", q);
        req.setAttribute("botEmail", ChatService.BOT_EMAIL); // JSP dung de nhan ra tin cua tro ly AI
        if (selected != null) {
            List<Message> messages = chatService.messagesAfter(selected.getId(), 0);
            req.setAttribute("messages", messages);
            req.setAttribute("lastId", messages.isEmpty() ? 0 : messages.get(messages.size() - 1).getId());
            req.setAttribute("orders", chatService.recentOrders(selected.getId()));
        }
        AdminView.render(req, resp, "admin-chat.jsp", "Chat với khách", "chat");
    }

    // Tin moi cua 1 khach (sau `after`) + the #poll hoi tiep. withHead = true khi vua gui tin: kem dau do che do de cap nhat o khung tren.
    private void forwardMessages(HttpServletRequest req, HttpServletResponse resp, Integer customerId, int after, boolean withHead)
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
        req.setAttribute("botEmail", ChatService.BOT_EMAIL);
        req.setAttribute("withHead", withHead);
        req.getRequestDispatcher(VIEWS + "admin-chat-messages.jsp").forward(req, resp);
    }

    // Bo loc hoi thoai: all | human (nhan vien dang tiep quan) | ai; gia tri la -> all
    private static String filterOf(HttpServletRequest req) {
        String f = req.getParameter("filter");
        return ("human".equals(f) || "ai".equals(f)) ? f : "all";
    }
}
