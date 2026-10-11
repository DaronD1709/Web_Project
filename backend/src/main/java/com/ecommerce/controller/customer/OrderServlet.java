package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Order;
import com.ecommerce.dto.PageResult;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.OrderService;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Lịch sử/chi tiết chỉ đọc; thao tác huỷ/hoàn hàng sẽ nối ở nhánh theo dõi đơn. */
@WebServlet({"/orders", "/orders/detail"})
public class OrderServlet extends HttpServlet {
    private final OrderService orderService = new OrderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
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
            req.setAttribute("order", orderService.getOwnOrder(customer, id));
            req.setAttribute("activeAccount", "orders");
            req.getRequestDispatcher("/WEB-INF/views/customer/order-detail.jsp").forward(req, resp);
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "no-store");
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}
