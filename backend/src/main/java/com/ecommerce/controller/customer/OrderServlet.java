package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Order;
import com.ecommerce.dto.PageResult;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import com.ecommerce.util.HtmxUtil;
import com.ecommerce.util.TrackingCsrf;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Lịch sử, theo dõi và thao tác đơn của Customer. */
@WebServlet({"/orders", "/orders/detail", "/orders/cancel", "/orders/return"})
public class OrderServlet extends HttpServlet {
    private final OrderService orderService = new OrderService();
    private static final String FLASH = "orderTrackingFlash";
    private record TrackingFlash(int id, String message) implements java.io.Serializable { }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        if ("/orders/cancel".equals(req.getServletPath()) || "/orders/return".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Customer customer = SessionUtil.requireCustomer(req, resp);
        if (customer == null) return;
        if ("/orders/detail".equals(req.getServletPath())) {
            showDetail(req, resp, customer);
            return;
        }
        try {
            String status = orderService.normalizeHistoryStatus(singleParameter(req, "status"));
            String rawPage = singleParameter(req, "page");
            Integer page = ParamUtil.intOrNull(rawPage);
            if (rawPage == null || rawPage.isBlank()) page = 1;
            if (page == null || page < 1) throw new BusinessException("Trang đơn hàng không hợp lệ.");
            PageResult<Order> result = orderService.getOwnHistory(customer, status, page);
            req.setAttribute("result", result);
            req.setAttribute("orders", result.getItems());
            req.setAttribute("selectedStatus", status);
            req.setAttribute("activeAccount", "orders");
            String view = "true".equals(req.getHeader("HX-Request"))
                    && "order-history".equals(req.getHeader("HX-Target"))
                    ? "/WEB-INF/views/customer/fragments/order-history.jsp"
                    : "/WEB-INF/views/customer/orders.jsp";
            req.getRequestDispatcher(view).forward(req, resp);
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, Customer customer)
            throws ServletException, IOException {
        Integer id;
        try {
            id = ParamUtil.intOrNull(singleParameter(req, "id"));
            if (id == null || id < 1) throw new BusinessException("Mã đơn hàng không hợp lệ.");
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }
        try {
            prepareDetail(req, customer, id);
            boolean fragment = isTrackingRequest(req);
            if (!fragment) {
                TrackingFlash flash = (TrackingFlash) req.getSession(false).getAttribute(FLASH);
                if (flash != null && flash.id() == id) {
                    req.setAttribute("trackingMessage", flash.message());
                    req.getSession(false).removeAttribute(FLASH);
                }
            }
            req.getRequestDispatcher(fragment
                    ? "/WEB-INF/views/customer/fragments/order-tracking.jsp"
                    : "/WEB-INF/views/customer/order-detail.jsp").forward(req, resp);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String singleParameter(HttpServletRequest req, String name) {
        String[] values = req.getParameterValues(name);
        if (values == null) return null;
        if (values.length != 1) throw new BusinessException("Tham số đơn hàng không hợp lệ.");
        return values[0];
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        boolean returning = "/orders/return".equals(req.getServletPath());
        if (!returning && !"/orders/cancel".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        Customer customer = SessionUtil.requireCustomer(req, resp);
        if (customer == null) return;
        Integer id;
        String reason;
        try {
            id = ParamUtil.intOrNull(singleParameter(req, "id"));
            if (id == null || id < 1) throw new BusinessException("Mã đơn hàng không hợp lệ.");
            reason = returning ? singleParameter(req, "reason") : null;
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
        // Kiểm tra quyền cho cả request lỗi; Service vẫn kiểm tra lại dưới khoá khi ghi.
        try {
            prepareDetail(req, customer, id);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (BusinessException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String message = returning ? "Đã gửi yêu cầu hoàn hàng." : "Đơn hàng đã được huỷ.";
        try {
            if (returning) orderService.requestOwnReturn(customer, id, reason);
            else orderService.cancelOwnOrder(customer, id);
        } catch (SecurityException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (BusinessException e) {
            prepareDetail(req, customer, id);
            req.setAttribute("trackingError", e.getMessage());
            if (returning) {
                req.setAttribute("returnError", e.getMessage());
                req.setAttribute("returnReason", reason);
            }
            if (HtmxUtil.isHtmx(req)) {
                resp.setStatus(422);
                HtmxUtil.toast(resp, e.getMessage(), "error");
            }
            req.getRequestDispatcher(HtmxUtil.isHtmx(req)
                    ? "/WEB-INF/views/customer/fragments/order-tracking.jsp"
                    : "/WEB-INF/views/customer/order-detail.jsp").forward(req, resp);
            return;
        }
        if (!HtmxUtil.isHtmx(req)) {
            req.getSession(false).setAttribute(FLASH, new TrackingFlash(id, message));
            resp.sendRedirect(req.getContextPath() + "/orders/detail?id=" + id);
            return;
        }
        prepareDetail(req, customer, id);
        req.setAttribute("trackingMessage", message);
        HtmxUtil.toast(resp, message, "success");
        req.getRequestDispatcher("/WEB-INF/views/customer/fragments/order-tracking.jsp").forward(req, resp);
    }

    private void prepareDetail(HttpServletRequest req, Customer customer, int id) {
        Order order = orderService.getOwnOrder(customer, id);
        req.setAttribute("order", order);
        req.setAttribute("activeAccount", "orders");
        req.setAttribute("canCancel", order.canMoveTo(com.ecommerce.entity.OrderStatus.CANCELLED));
        req.setAttribute("canReturn", order.canMoveTo(com.ecommerce.entity.OrderStatus.RETURN_REQUESTED));
        TrackingCsrf.prepare(req);
    }

    private boolean isTrackingRequest(HttpServletRequest req) {
        return HtmxUtil.isHtmx(req) && "order-detail-body".equals(req.getHeader("HX-Target"));
    }
}
