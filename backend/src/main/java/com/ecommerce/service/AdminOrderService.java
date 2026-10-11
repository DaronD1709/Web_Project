package com.ecommerce.service;

import com.ecommerce.dao.OrderDAO;
import com.ecommerce.dto.AdminOrderFilter;
import com.ecommerce.dto.PageResult;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Nghiep vu QUAN LY DON HANG cua Admin: xem danh sach/chi tiet, xac nhan, giao, hoan tat, huy, duyet hoac tu choi hoan hang. */
public class AdminOrderService {

    private final OrderDAO orderDAO = new OrderDAO();

    /** 1 trang don hang khop bo loc: 1 query lay don cua trang + 1 query dem tong de tinh so trang. */
    public PageResult<Order> search(AdminOrderFilter filter) {
        long total = orderDAO.countSearch(filter);
        List<Order> items = orderDAO.search(filter);
        return new PageResult<>(items, total, filter.getPage(), filter.getPageSize());
    }

    /** So don cho tung tab: khoa "all", "pending", ... , "return" (= yeu cau hoan + da hoan). JSP doc bang ${tabCounts['pending']}. */
    public Map<String, Long> tabCounts() {
        Map<String, Long> counts = new HashMap<>();
        long all = 0;
        for (Map.Entry<OrderStatus, Long> e : orderDAO.countByStatus().entrySet()) {
            counts.put(e.getKey().name().toLowerCase(), e.getValue());
            all += e.getValue();
        }
        counts.put("all", all);
        counts.put("return", counts.getOrDefault("return_requested", 0L) + counts.getOrDefault("returned", 0L));
        return counts;
    }

    /** id don -> so mat hang, cho cac don dang hien trong danh sach. */
    public Map<Integer, Long> countItems(List<Order> orders) {
        return orderDAO.countItems(orders.stream().map(Order::getId).toList());
    }

    public Order getDetail(Integer id) {
        Order order = id == null ? null : orderDAO.findDetail(id);
        if (order == null) throw new BusinessException("Đơn hàng không tồn tại.");
        return order;
    }

    /**
     * Doi trang thai don. nextName la ten OrderStatus tu form (vd "CONFIRMED"). Tra ve cau thong bao de hien cho Admin.
     * Cac buoc: (1) doc trang thai dich  (2) don co that khong  (3) Admin co duoc phep chuyen vao trang thai do  (4) luat chuyen hop le
     * (5) ghi DB + hoan kho + thong bao cho khach (OrderDAO.applyStatusChange).
     */
    public String changeStatus(Integer orderId, String nextName) {
        OrderStatus next;
        try {
            next = OrderStatus.valueOf(nextName == null ? "" : nextName.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái không hợp lệ.");
        }
        // Yeu cau hoan hang la viec cua KHACH, Admin khong tu dat trang thai nay
        if (next == OrderStatus.RETURN_REQUESTED || next == OrderStatus.PENDING) {
            throw new BusinessException("Quản trị viên không thể chuyển đơn sang trạng thái này.");
        }

        Order order = getDetail(orderId);
        if (!order.canMoveTo(next)) {
            throw new BusinessException("Không thể chuyển đơn #" + orderId + " từ \"" + order.getStatus().getLabel()
                    + "\" sang \"" + next.getLabel() + "\".");
        }

        // Cung la COMPLETED nhung khac y nghia: tu SHIPPING = giao xong; tu RETURN_REQUESTED = tu choi hoan hang
        boolean rejectReturn = order.getStatus() == OrderStatus.RETURN_REQUESTED && next == OrderStatus.COMPLETED;
        String text = switch (next) {
            case CONFIRMED -> "Đơn hàng #" + orderId + " đã được xác nhận.";
            case SHIPPING -> "Đơn hàng #" + orderId + " đang được giao đến bạn.";
            case COMPLETED -> rejectReturn ? "Yêu cầu hoàn hàng của đơn #" + orderId + " đã bị từ chối."
                    : "Đơn hàng #" + orderId + " đã giao thành công. Cảm ơn bạn!";
            case CANCELLED -> "Đơn hàng #" + orderId + " đã bị huỷ bởi cửa hàng.";
            default -> "Yêu cầu hoàn hàng của đơn #" + orderId + " đã được duyệt.";
        };
        orderDAO.applyStatusChange(orderId, next, text);
        return rejectReturn ? "Đã từ chối yêu cầu hoàn hàng của đơn #" + orderId : "Đơn #" + orderId + ": " + next.getLabel();
    }
}
