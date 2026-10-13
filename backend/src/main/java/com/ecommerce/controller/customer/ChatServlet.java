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
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

// GET /chat -> trang chat;  GET /chat/messages?after=ID -> tin moi (htmx polling moi 3 giay);  POST /chat/send -> gui tin;  POST /chat/guest -> khach vang lai nhap ten de bat dau
// Khong dung WebSocket: trinh duyet tu hoi server dinh ky (xem docs/api-spec.md muc 6).
// 2 loai nguoi chat:
//   1. KHACH CO TAI KHOAN (da dang nhap): dung chinh tai khoan do (ten, email... lay tu tai khoan), lich su chat gan voi tai khoan.
//   2. KHACH VANG LAI (chua dang nhap): phai nhap TEN truoc (trang chat-start.jsp), server tao 1 "khach vang lai" (xem User.guest) va nho id trong SESSION;
//      dong tab/het phien thi mat cuoc chat nay. Admin thay ten khach kem nhan "Khach vang lai".
@WebServlet({"/chat", "/chat/*"})
public class ChatServlet extends HttpServlet {

    private static final String PAGE = "/WEB-INF/views/customer/chat.jsp";
    private static final String START_PAGE = "/WEB-INF/views/customer/chat-start.jsp";
    private static final String FRAGMENT = "/WEB-INF/views/customer/fragments/chat-messages.jsp";
    private static final String GUEST_SESSION_KEY = "guestChatId";

    private final ChatService chatService = new ChatService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean loggedIn = SessionUtil.currentUser(req) != null;
        Customer owner = loggedIn ? SessionUtil.requireCustomer(req, resp) : guestFromSession(req);
        if (loggedIn && owner == null) return; // requireCustomer da tra loi (chuyen /login hoac 403)
        if (owner == null) { // chua dang nhap va chua nhap ten
            showStart(req, resp, null, "/messages".equals(req.getPathInfo()));
            return;
        }
        if ("/messages".equals(req.getPathInfo())) {
            forwardMessages(req, resp, owner, ParamUtil.intOr(req.getParameter("after"), 0), FRAGMENT);
        } else {
            forwardMessages(req, resp, owner, 0, PAGE);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String path = req.getPathInfo();
        if ("/guest".equals(path)) {
            startGuest(req, resp);
            return;
        }
        if (!"/send".equals(path)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        boolean loggedIn = SessionUtil.currentUser(req) != null;
        Customer owner = loggedIn ? SessionUtil.requireCustomer(req, resp) : guestFromSession(req);
        if (loggedIn && owner == null) return;
        if (owner == null) { // phien het han / chua nhap ten: quay ve trang nhap ten
            showStart(req, resp, null, true);
            return;
        }
        try {
            chatService.sendFromCustomer(owner.getId(), req.getParameter("content"));
        } catch (BusinessException e) {
            HtmxUtil.toast(resp, e.getMessage(), "error");
            resp.setHeader("HX-Reswap", "none");
            return;
        }
        // Tra ve tin moi tu luc form biet (after) -> gom ca tin vua gui; htmx chen vao khung chat
        forwardMessages(req, resp, owner, ParamUtil.intOr(req.getParameter("after"), 0), FRAGMENT);
    }

    // Khach chua dang nhap nhap ten -> tao khach vang lai, nho id trong session, chuyen ve /chat. Da dang nhap thi khong can buoc nay.
    private void startGuest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.currentUser(req) != null) {
            resp.sendRedirect(req.getContextPath() + "/chat");
            return;
        }
        try {
            Customer guest = chatService.startGuest(req.getParameter("name"));
            req.getSession().setAttribute(GUEST_SESSION_KEY, guest.getId());
            resp.sendRedirect(req.getContextPath() + "/chat");
        } catch (BusinessException e) {
            resp.setStatus(422);
            showStart(req, resp, e.getMessage(), false);
        }
    }

    // Khach vang lai theo id luu trong session; null neu chua co / khong con hop le (vd DB bi xoa)
    private Customer guestFromSession(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object id = session == null ? null : session.getAttribute(GUEST_SESSION_KEY);
        return id instanceof Integer guestId ? chatService.findGuest(guestId) : null;
    }

    // Trang nhap ten. Yeu cau cua htmx (polling/gui tin) thi chuyen ca trang ve /chat thay vi nhet trang vao khung chat.
    private void showStart(HttpServletRequest req, HttpServletResponse resp, String error, boolean htmxRequest) throws ServletException, IOException {
        if (htmxRequest && HtmxUtil.isHtmx(req)) {
            HtmxUtil.redirect(resp, req.getContextPath() + "/chat");
            return;
        }
        req.setAttribute("error", error);
        req.setAttribute("guestName", req.getParameter("name"));
        req.getRequestDispatcher(START_PAGE).forward(req, resp);
    }

    private void forwardMessages(HttpServletRequest req, HttpServletResponse resp, Customer owner, int after, String view)
            throws ServletException, IOException {
        List<Message> messages = chatService.getMessagesAfter(owner.getId(), after);
        int lastId = messages.isEmpty() ? after : messages.get(messages.size() - 1).getId();
        req.setAttribute("messages", messages);
        req.setAttribute("lastId", lastId);
        req.setAttribute("botEmail", ChatService.BOT_EMAIL); // JSP dung de nhan ra tin cua tro ly tu dong
        req.setAttribute("chatOwnerId", owner.getId());       // de JSP biet tin nao la CUA MINH (khach vang lai khong co currentUser trong session)
        req.setAttribute("isGuestChat", owner.isGuest());
        req.setAttribute("chatOwner", owner);
        req.getRequestDispatcher(view).forward(req, resp);
    }
}
