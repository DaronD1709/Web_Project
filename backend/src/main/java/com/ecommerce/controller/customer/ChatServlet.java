package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Message;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.ChatService;
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

// GET /chat -> trang chat;  GET /chat/messages?after=ID -> tin moi (htmx polling moi 3 giay);  POST /chat/send -> gui tin
// Khong dung WebSocket: trinh duyet tu hoi server dinh ky (xem docs/api-spec.md muc 6).
@WebServlet({"/chat", "/chat/*"})
public class ChatServlet extends HttpServlet {

    private static final String PAGE = "/WEB-INF/views/customer/chat.jsp";
    private static final String FRAGMENT = "/WEB-INF/views/customer/fragments/chat-messages.jsp";

    private final ChatService chatService = new ChatService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) return;
        if ("/messages".equals(req.getPathInfo())) {
            forwardMessages(req, resp, user, ParamUtil.intOr(req.getParameter("after"), 0), FRAGMENT);
        } else {
            forwardMessages(req, resp, user, 0, PAGE);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null || !"/send".equals(req.getPathInfo())) {
            if (user != null) resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            chatService.sendFromCustomer(user.getId(), req.getParameter("content"));
        } catch (BusinessException e) {
            HtmxUtil.toast(resp, e.getMessage(), "error");
            resp.setHeader("HX-Reswap", "none");
            return;
        }
        // Tra ve tin moi tu luc form biet (after) -> gom ca tin vua gui; htmx chen vao khung chat
        forwardMessages(req, resp, user, ParamUtil.intOr(req.getParameter("after"), 0), FRAGMENT);
    }

    private void forwardMessages(HttpServletRequest req, HttpServletResponse resp, Customer user, int after, String view)
            throws ServletException, IOException {
        List<Message> messages = chatService.getMessagesAfter(user.getId(), after);
        int lastId = messages.isEmpty() ? after : messages.get(messages.size() - 1).getId();
        req.setAttribute("messages", messages);
        req.setAttribute("lastId", lastId);
        req.setAttribute("botEmail", ChatService.BOT_EMAIL); // JSP dung de nhan ra tin cua tro ly AI
        req.getRequestDispatcher(view).forward(req, resp);
    }
}
