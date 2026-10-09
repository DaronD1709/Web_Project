package com.ecommerce.controller.admin;

import com.ecommerce.service.AdminStatsService;
import com.ecommerce.util.AdminView;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// Thong ke doanh thu (Admin).
//   GET /admin/statistics?days=7|14|30   so lieu + bieu do doanh thu theo ngay / theo danh muc + top san pham trong N ngay qua (mac dinh 30)
//   GET /admin/statistics/export?days=   tai file CSV doanh thu theo ngay
@WebServlet("/admin/statistics/*")
public class AdminStatisticsServlet extends HttpServlet {

    private final AdminStatsService statsService = new AdminStatsService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403
        String path = req.getPathInfo() == null ? "" : req.getPathInfo(); // phan URL sau "/admin/statistics"
        int days = daysOf(req);

        switch (path) {
            case "", "/" -> showPage(req, resp, days);
            case "/export" -> {
                resp.setContentType("text/csv;charset=UTF-8");
                resp.setHeader("Content-Disposition", "attachment; filename=\"doanh-thu-" + days + "-ngay.csv\"");
                resp.getOutputStream().write(statsService.revenueCsv(days).getBytes(StandardCharsets.UTF_8));
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showPage(HttpServletRequest req, HttpServletResponse resp, int days) throws ServletException, IOException {
        // Ky nay va ky truoc lien ke co do dai bang nhau -> so sanh % tang/giam
        double[] now = statsService.totals(days, 0);
        double[] before = statsService.totals(days, days);
        double revenue = now[0], orders = now[1];
        double avgNow = orders == 0 ? 0 : revenue / orders;
        double avgBefore = before[1] == 0 ? 0 : before[0] / before[1];

        req.setAttribute("days", days);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        req.setAttribute("rangeText", LocalDate.now().minusDays(days - 1L).format(f) + " → " + LocalDate.now().format(f));
        req.setAttribute("revenue", revenue);
        req.setAttribute("revenueDelta", AdminStatsService.deltaPercent(revenue, before[0]));
        req.setAttribute("orders", (long) orders);
        req.setAttribute("ordersDelta", AdminStatsService.deltaPercent(orders, before[1]));
        req.setAttribute("avgOrder", avgNow);
        req.setAttribute("avgDelta", AdminStatsService.deltaPercent(avgNow, avgBefore));
        req.setAttribute("cancelled", (long) now[2]);
        req.setAttribute("cancelRate", now[3] == 0 ? 0 : now[2] / now[3] * 100); // % don huy tren tong don cua ky

        req.setAttribute("revenueChart", statsService.revenueByDay(days));
        req.setAttribute("categoryChart", statsService.revenueByCategory(days));
        req.setAttribute("topProducts", statsService.topProducts(days, 10));
        AdminView.render(req, resp, "admin-statistics.jsp", "Thống kê doanh thu", "statistics");
    }

    // Chi nhan 7, 14 hoac 30 ngay (gia tri la -> 30)
    private static int daysOf(HttpServletRequest req) {
        int d = ParamUtil.intOr(req.getParameter("days"), 30);
        return (d == 7 || d == 14) ? d : 30;
    }
}
