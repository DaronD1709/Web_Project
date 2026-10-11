package com.ecommerce.controller.admin;

import com.ecommerce.dto.AdminOrderFilter;
import com.ecommerce.dto.ChartData;
import com.ecommerce.dto.ChartPoint;
import com.ecommerce.entity.Message;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.service.AdminChatService;
import com.ecommerce.service.AdminOrderService;
import com.ecommerce.service.AdminStatsService;
import com.ecommerce.util.AdminView;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

// Trang Tong quan cua CMS: GET /admin. Gom so lieu nhanh de Admin biet can lam gi truoc (don cho, hoan hang, tin nhan, hang sap het)
// va xem doanh thu 14 ngay. Thong ke chi tiet o /admin/statistics (AdminStatisticsServlet).
@WebServlet({"/admin", "/admin/"})
public class AdminDashboardServlet extends HttpServlet {

    private static final int REVENUE_DAYS = 14;

    private final AdminStatsService statsService = new AdminStatsService();
    private final AdminOrderService orderService = new AdminOrderService();
    private final AdminChatService chatService = new AdminChatService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (SessionUtil.requireAdmin(req, resp) == null) return; // chua dang nhap -> /admin/login, khach hang -> 403

        // (1) Viec can xu ly ngay: don cho xac nhan, yeu cau hoan hang, khach dang cho nhan vien tra loi
        req.setAttribute("pending", statsService.countStatus(OrderStatus.PENDING));
        req.setAttribute("returnRequests", statsService.countStatus(OrderStatus.RETURN_REQUESTED));
        long chatWaiting = chatService.conversations("all", null).stream()
                .filter((Message m) -> m.getSender().getId().equals(m.getCustomer().getId())) // tin cuoi la cua khach = shop chua tra loi
                .count();
        req.setAttribute("chatWaiting", chatWaiting);

        // (2) 4 the so lieu: doanh thu hom nay (so voi hom qua), don dang giao, san pham sap het
        double[] today = statsService.todayAndYesterday();
        req.setAttribute("todayRevenue", today[0]);
        req.setAttribute("revenueDelta", AdminStatsService.deltaPercent(today[0], today[1])); // null neu hom qua = 0
        req.setAttribute("shipping", statsService.countStatus(OrderStatus.SHIPPING));
        req.setAttribute("lowCount", statsService.countLowStock());

        // (3) Bieu do, danh sach
        req.setAttribute("revenueChart", statsService.revenueByDay(REVENUE_DAYS));
        req.setAttribute("revenueDays", REVENUE_DAYS);
        req.setAttribute("statusChart", statsService.ordersByStatus());
        var latest = orderService.search(new AdminOrderFilter()).getItems(); // trang 1 = don moi nhat
        req.setAttribute("recentOrders", latest.subList(0, Math.min(6, latest.size())));
        req.setAttribute("lowStock", statsService.lowStock());
        var top = statsService.topProducts(30, 6); // ban chay 30 ngay qua; ve thanh ngang theo so luong
        req.setAttribute("topChart", new ChartData(top.stream().map(t -> new ChartPoint(t.getName(), t.getQuantity(), t.getCategoryName())).toList()));

        AdminView.render(req, resp, "admin-dashboard.jsp", "Tổng quan", "dashboard");
    }
}
