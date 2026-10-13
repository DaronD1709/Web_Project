package com.ecommerce.dao;

import com.ecommerce.entity.Notification;
import com.ecommerce.entity.Order;
import com.ecommerce.util.JPAUtil;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.entity.VNPayPayment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

/** Transaction chung cho IPN/QueryDr; chữ ký và nghiệp vụ nằm ở Service. */
public class VNPayDAO {

    // Gói kết quả trả về, không phải entity hay bảng dữ liệu mới.
    public record IpnResult(
            String responseCode,
            Order orderForEmail,
            String notificationText) {
    }

    @FunctionalInterface
    public interface IpnUpdater {
        IpnResult update(Order order);
    }

    public IpnResult processIpn(
            Integer orderId, IpnUpdater updater) {
        EntityManager em =
                JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            // Khoá cùng dòng Order mà Admin khoá khi đổi trạng thái.
            List<Order> matches = em.createQuery(
                    "SELECT o FROM Order o WHERE o.id = :id",
                    Order.class)
                    .setParameter("id", orderId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();

            if (matches.isEmpty()) {
                tx.commit();
                return new IpnResult("01", null, null);
            }

            Order order = matches.get(0);

            if (order.getPayment() != null) {
                // Đọc lại thanh toán sau khi đã có khoá đơn.
                em.refresh(order.getPayment());
            }

            // Fetch đủ dữ liệu để dựng email sau khi transaction kết thúc.
            order = em.createQuery(
                    "SELECT DISTINCT o FROM Order o "
                    + "JOIN FETCH o.customer "
                    + "LEFT JOIN FETCH o.items i "
                    + "LEFT JOIN FETCH i.product "
                    + "LEFT JOIN FETCH o.payment "
                    + "LEFT JOIN FETCH o.shippingAddress "
                    + "LEFT JOIN FETCH o.voucher "
                    + "WHERE o.id = :id",
                    Order.class)
                    .setParameter("id", orderId)
                    .getSingleResult();

            // Service truyền phần xử lý vào.
            // Callback chạy ngay trong transaction, không chạy nền.
            IpnResult result = updater.update(order);

            // Chỉ huỷ sau khi Service ghi nhận một kết quả thất bại hợp lệ.
            // "00" ở đây nghĩa là đã xử lý IPN, không phải thanh toán thành công.
            boolean cancelFailedOrder = "00".equals(result.responseCode())
                    && order.getPayment() instanceof VNPayPayment
                    && order.getPayment().getStatus() == PaymentStatus.FAILED
                    && order.canMoveTo(OrderStatus.CANCELLED);

            if (cancelFailedOrder) {
                // Dùng chính transaction IPN hiện tại.
                // Helper đã tạo Notification nên không tạo thêm lần nữa.
                new OrderDAO().changeStatusInTransaction(
                        em, order, OrderStatus.CANCELLED,
                        result.notificationText());

            } else if (result.notificationText() != null) {
                Notification notification = new Notification();
                notification.setUser(order.getCustomer());
                notification.setMessage(result.notificationText());
                notification.setRead(false);
                notification.setCreatedAt(LocalDateTime.now());
                em.persist(notification);
            }

            // Chỉ trả kết quả cho Service sau khi lưu thành công.
            tx.commit();
            return result;

        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;

        } finally {
            em.close();
        }
    }
}
