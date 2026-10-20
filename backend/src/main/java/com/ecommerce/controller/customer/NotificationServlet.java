package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.NotificationService;
import com.ecommerce.util.HtmxUtil;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import com.ecommerce.util.TrackingCsrf;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

// Thong bao cua KHACH (phia khach cua chuc nang Quan ly don hang: Admin doi trang thai don -> OrderDAO tao Notification -> khach xem o day).
//   GET  /notifications         trang danh sach (loc Tat ca / Chua doc, phan trang); htmx (HX-Target = notification-list) chi tra manh danh sach
//   GET  /notifications/count   huy hieu so thong bao chua doc tren chuong o header (htmx hoi lai moi 1 giay)
//   POST /notifications/read    danh dau 1 hoac tat ca da doc (co ma CSRF)
// Servlet chi dieu phoi; nghiep vu o NotificationService, du lieu o NotificationDAO.
@WebServlet({"/notifications", "/notifications/count", "/notifications/read"})
public class NotificationServlet extends HttpServlet {
    private final NotificationService service = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        if ("/notifications/read".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) return;
        if ("/notifications/count".equals(req.getServletPath())) {
            prepareCount(req, user);
            req.getRequestDispatcher("/WEB-INF/views/customer/fragments/notification-count.jsp").forward(req, resp);
            return;
        }
        try {
            prepareList(req, user, service.normalizeFilter(single(req, "filter")), page(req));
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        req.getRequestDispatcher(HtmxUtil.isHtmx(req) && "notification-list".equals(req.getHeader("HX-Target"))
                ? "/WEB-INF/views/customer/fragments/notification-list.jsp"
                : "/WEB-INF/views/customer/notifications.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        if (!"/notifications/read".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Customer user = SessionUtil.requireCustomer(req, resp);
        if (user == null) return;
        String filter;
        int page;
        Integer id;
        boolean all;
        try {
            filter = service.normalizeFilter(single(req, "filter"));
            page = page(req);
            String rawId = single(req, "id"), rawAll = single(req, "all");
            all = "true".equals(rawAll);
            id = ParamUtil.intOrNull(rawId);
            if ((rawAll != null && !all) || (all && rawId != null)
                    || (!all && (id == null || id < 1))) throw new BusinessException("Thao tác đọc thông báo không hợp lệ.");
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        if (!TrackingCsrf.isValid(req)) {
            if (HtmxUtil.isHtmx(req)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setHeader("HX-Reswap", "none");
                HtmxUtil.toast(resp, "Phiên gửi biểu mẫu không hợp lệ. Vui lòng tải lại trang.", "error");
            } else resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            service.markOwnRead(user, id, all);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!HtmxUtil.isHtmx(req)) {
            resp.sendRedirect(req.getContextPath() + "/notifications?filter=" + filter + "&page=" + page);
            return;
        }
        prepareList(req, user, filter, page);
        req.setAttribute("updateNotificationCount", true);
        HtmxUtil.toast(resp, all ? "Đã đánh dấu tất cả thông báo đã đọc." : "Đã đánh dấu thông báo đã đọc.", "success");
        req.getRequestDispatcher("/WEB-INF/views/customer/fragments/notification-list.jsp").forward(req, resp);
    }

    private void prepareList(HttpServletRequest req, Customer user, String filter, int page) {
        req.setAttribute("result", service.getOwn(user, filter, page));
        req.setAttribute("selectedFilter", filter);
        req.setAttribute("activeAccount", "notifications");
        prepareCount(req, user);
        TrackingCsrf.prepare(req);
    }

    private void prepareCount(HttpServletRequest req, Customer user) {
        req.setAttribute("unreadCount", service.countUnread(user));
        req.setAttribute("notificationCountLoaded", true);
    }

    private int page(HttpServletRequest req) {
        String raw = single(req, "page");
        Integer page = ParamUtil.intOrNull(raw);
        if (raw == null || raw.isBlank()) page = 1;
        if (page == null || page < 1) throw new BusinessException("Trang thông báo không hợp lệ.");
        return page;
    }

    private String single(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        if (values == null) return null;
        if (values.length != 1) throw new BusinessException("Tham số thông báo không hợp lệ.");
        return values[0];
    }
}
