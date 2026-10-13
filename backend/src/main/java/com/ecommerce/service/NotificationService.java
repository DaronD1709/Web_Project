package com.ecommerce.service;

import com.ecommerce.dao.NotificationDAO;
import com.ecommerce.dto.PageResult;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Notification;
import java.util.Locale;

/** Đọc thông báo đã được OrderDAO/Checkout/VNPay tạo; không tạo thông báo lần nữa. */
public class NotificationService {
    private final NotificationDAO dao = new NotificationDAO();

    public String normalizeFilter(String value) {
        String filter = value == null || value.isBlank() ? "ALL" : value.trim().toUpperCase(Locale.ROOT);
        if (!"ALL".equals(filter) && !"UNREAD".equals(filter)) throw new BusinessException("Bộ lọc thông báo không hợp lệ.");
        return filter;
    }

    public PageResult<Notification> getOwn(Customer user, String filter, int page) {
        requireCustomer(user);
        if (page < 1) throw new BusinessException("Trang thông báo không hợp lệ.");
        return dao.findOwn(user.getId(), "UNREAD".equals(normalizeFilter(filter)), page);
    }

    public long countUnread(Customer user) {
        requireCustomer(user);
        return dao.countUnread(user.getId());
    }

    public void markOwnRead(Customer user, Integer id, boolean all) {
        requireCustomer(user);
        if (!all) {
            if (id == null || id < 1) throw new BusinessException("Mã thông báo không hợp lệ.");
            Notification notification = dao.findById(id);
            if (notification == null) throw new BusinessException("Thông báo không tồn tại.");
            if (!user.getId().equals(notification.getUser().getId())) {
                throw new SecurityException("Bạn không có quyền đọc thông báo này.");
            }
        } else if (id != null) {
            throw new BusinessException("Chỉ chọn một thông báo hoặc tất cả.");
        }
        dao.markRead(user.getId(), all ? null : id);
    }

    private void requireCustomer(Customer user) {
        if (user == null || user.getId() == null || !user.isActive()) {
            throw new BusinessException("Vui lòng đăng nhập để xem thông báo.");
        }
    }
}
