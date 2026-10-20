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
// GET /chat/widget -> than khung chat nho cua BONG BONG CHAT o goc phai moi trang (common/footer.jspf): cac tin nhan + o nhap, hoac o nhap ten neu la khach chua dang nhap.
//   Bong bong dung lai CHINH cac endpoint tren (/chat/messages, /chat/send, /chat/guest); chi khac la ket qua duoc nhet vao khung nho thay vi trang rieng.
// Khong dung WebSocket: trinh duyet tu hoi server dinh ky (xem docs/api-spec.md muc 6).
// 2 loai nguoi chat:
//   1. KHACH CO TAI KHOAN (da dang nhap): dung chinh tai khoan do (ten, email... lay tu tai khoan), lich su chat gan voi tai khoan.
//   2. KHACH VANG LAI (chua dang nhap): phai nhap TEN truoc (trang chat-start.jsp), server tao 1 "khach vang lai" (xem User.guest) va nho id trong SESSION;
//      dong tab/het phien thi mat cuoc chat nay. Admin thay ten khach kem nhan "Khach vang lai".
@WebServlet({"/chat", "/chat/*"})
public class ChatServlet extends HttpServlet {

    // 3 view cua khach: PAGE = trang chat day du; START_PAGE = trang nhap ten (khach vang lai); FRAGMENT = chi cac tin moi (htmx polling/gui tin).
    private static final String PAGE = "/WEB-INF/views/customer/chat.jsp";
    private static final String START_PAGE = "/WEB-INF/views/customer/chat-start.jsp";
    private static final String FRAGMENT = "/WEB-INF/views/customer/fragments/chat-messages.jsp";
    // Than khung chat nho cua bong bong chat (tin nhan + o nhap, hoac o nhap ten).
    private static final String WIDGET = "/WEB-INF/views/customer/fragments/chat-widget.jsp";
    // Ten khoa trong session luu id khach vang lai; het phien (dong trinh duyet) la mat khoa nay va mat cuoc chat.
    private static final String GUEST_SESSION_KEY = "guestChatId";

    private final ChatService chatService = new ChatService();

    // doGet: 2 duong. "/chat" -> ve CA TRANG (lan dau, after = 0 la lay tu dau).
    //                 "/chat/messages?after=N" -> chi tra tin moi hon N (polling moi 3 giay, trang ve manh HTML).
    // Xac dinh "ai la chu cuoc chat": dang nhap thi la Customer cua session, chua dang nhap thi tim khach vang lai trong session.
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/widget".equals(req.getPathInfo())) { // khung chat nho cua bong bong chat
            showWidget(req, resp);
            return;
        }
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

    // doPost: 2 thao tac ghi: "/guest" (khach vang lai nhap ten de bat dau) va "/send" (gui 1 tin).
    // Gui tin: ChatService luu tin (nhan vien tra loi bang tay), roi tra ve cac tin moi de htmx chen vao khung chat.
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
            // Tin rong/qua dai: chi hien toast loi; "HX-Reswap: none" de khung chat va o nhap giu nguyen.
            HtmxUtil.toast(resp, e.getMessage(), "error");
            resp.setHeader("HX-Reswap", "none");
            return;
        }
        // Tra ve tin moi tu luc form biet (after) -> gom ca tin vua gui; htmx chen vao khung chat
        forwardMessages(req, resp, owner, ParamUtil.intOr(req.getParameter("after"), 0), FRAGMENT);
    }

    // Khach chua dang nhap nhap ten -> tao khach vang lai, nho id trong session, chuyen ve /chat. Da dang nhap thi khong can buoc nay.
    private void startGuest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Yeu cau tu bong bong chat: form o chat-widget.jsp gui bang htmx va nhan ket qua vao #nvw-body (HX-Target = nvw-body).
        // Khi do phai tra ve MANH HTML cua khung nho, khong duoc redirect ve trang /chat (htmx se nhet ca trang vao trong khung).
        boolean widget = HtmxUtil.isHtmx(req) && "nvw-body".equals(req.getHeader("HX-Target"));
        if (SessionUtil.currentUser(req) != null) {
            if (widget) showWidget(req, resp);
            else resp.sendRedirect(req.getContextPath() + "/chat");
            return;
        }
        try {
            Customer guest = chatService.startGuest(req.getParameter("name"));
            req.getSession().setAttribute(GUEST_SESSION_KEY, guest.getId());
            if (widget) forwardMessages(req, resp, guest, 0, WIDGET); // vao luon khung tin nhan cua khach vua tao
            else resp.sendRedirect(req.getContextPath() + "/chat");
        } catch (BusinessException e) {
            if (widget) {
                // Loi nhap ten: tra lai o nhap ten kem loi voi ma 200 (htmx mac dinh KHONG thay noi dung khi gap ma 4xx).
                req.setAttribute("widgetStart", true);
                req.setAttribute("error", e.getMessage());
                req.setAttribute("guestName", req.getParameter("name"));
                req.getRequestDispatcher(WIDGET).forward(req, resp);
            } else {
                resp.setStatus(422);
                showStart(req, resp, e.getMessage(), false);
            }
        }
    }

    // GET /chat/widget: than khung chat nho. Xac dinh chu cuoc chat nhu doGet: dang nhap thi la Customer cua session, chua thi tim khach vang lai trong session.
    //   - Chua co ai (chua dang nhap, chua nhap ten): hien o nhap ten (attr widgetStart = true).
    //   - Co chu cuoc chat: nap tu dau (after = 0) roi hien tin nhan + o nhap.
    // Admin goi vao day bi requireCustomer chan (403); bong bong cung khong hien voi Admin.
    private void showWidget(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean loggedIn = SessionUtil.currentUser(req) != null;
        Customer owner = loggedIn ? SessionUtil.requireCustomer(req, resp) : guestFromSession(req);
        if (loggedIn && owner == null) return; // requireCustomer da tra loi (chuyen /login hoac 403)
        if (owner == null) {
            req.setAttribute("widgetStart", true);
            req.getRequestDispatcher(WIDGET).forward(req, resp);
            return;
        }
        forwardMessages(req, resp, owner, 0, WIDGET);
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

    // Lay tin moi hon `after` roi chuyen cho view (PAGE hoac FRAGMENT). lastId = id tin cuoi, JSP gan vao the #poll de lan hoi sau chi xin tin moi.
    // chatOwnerId: JSP so sanh voi sender.id de biet tin nao la "cua minh" (khach vang lai khong co currentUser trong session nen khong dung duoc).
    private void forwardMessages(HttpServletRequest req, HttpServletResponse resp, Customer owner, int after, String view)
            throws ServletException, IOException {
        List<Message> messages = chatService.getMessagesAfter(owner.getId(), after);
        int lastId = messages.isEmpty() ? after : messages.get(messages.size() - 1).getId();
        req.setAttribute("messages", messages);
        req.setAttribute("lastId", lastId);
        req.setAttribute("chatOwnerId", owner.getId());       // de JSP biet tin nao la CUA MINH (khach vang lai khong co currentUser trong session)
        req.setAttribute("isGuestChat", owner.isGuest());
        req.setAttribute("chatOwner", owner);
        req.getRequestDispatcher(view).forward(req, resp);
    }
}
