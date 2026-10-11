package com.ecommerce.dao;

import com.ecommerce.dto.AdminOrderFilter;
import com.ecommerce.entity.Notification;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.util.JPAUtil;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Voucher;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Truy van don hang cho Admin (loc/phan trang, dem theo trang thai, chi tiet day du) va doi trang thai kem hoan kho trong 1 transaction. */
public class OrderDAO extends AbstractDAO<Order, Integer> {

    public OrderDAO() {
        super(Order.class);
    }

    // ------------------------------------------------------------------ danh sach

    /** 1 trang don hang khop bo loc, moi nhat truoc. JOIN FETCH khach + thanh toan de JSP doc duoc sau khi EntityManager dong. */
    public List<Order> search(AdminOrderFilter f) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            String jpql = "SELECT o FROM Order o JOIN FETCH o.customer c LEFT JOIN FETCH o.payment pm"
                    + buildWhere(f) + " ORDER BY o.id DESC";
            Query query = em.createQuery(jpql, Order.class);
            setParams(query, f);
            query.setFirstResult(f.getOffset());
            query.setMaxResults(f.getPageSize());
            @SuppressWarnings("unchecked")
            List<Order> result = query.getResultList();
            return result;
        } finally {
            em.close();
        }
    }

    /** Tong so don khop bo loc (khong phan trang) de tinh so trang. */
    public long countSearch(AdminOrderFilter f) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(o) FROM Order o JOIN o.customer c LEFT JOIN o.payment pm" + buildWhere(f), Long.class);
            setParams(query, f);
            return (Long) query.getSingleResult();
        } finally {
            em.close();
        }
    }

    // Chi noi cac DIEU KIEN CO DINH vao chuoi; gia tri nguoi dung nhap luon qua setParameter (chong JPQL injection).
    private String buildWhere(AdminOrderFilter f) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        switch (f.getStatus()) {
            case "all" -> { }
            case "return" -> w.append(" AND o.status IN (:retA, :retB)"); // tab "Hoan hang" gom yeu cau hoan + da hoan
            default -> w.append(" AND o.status = :status");
        }
        if (f.getKeyword() != null) {
            w.append(" AND (LOWER(c.fullName) LIKE :kw OR LOWER(c.email) LIKE :kw");
            if (keywordAsId(f) != null) w.append(" OR o.id = :kwId"); // go "1042" hoac "#1042" thi tim ca theo ma don
            w.append(")");
        }
        if ("cod".equals(f.getPay())) w.append(" AND TYPE(pm) = CODPayment");
        if ("vnpay".equals(f.getPay())) w.append(" AND TYPE(pm) = VNPayPayment");
        if (f.getFrom() != null) w.append(" AND o.orderDate >= :from");
        if (f.getTo() != null) w.append(" AND o.orderDate < :toExclusive");
        return w.toString();
    }

    private void setParams(Query q, AdminOrderFilter f) {
        switch (f.getStatus()) {
            case "all" -> { }
            case "return" -> {
                q.setParameter("retA", OrderStatus.RETURN_REQUESTED);
                q.setParameter("retB", OrderStatus.RETURNED);
            }
            default -> q.setParameter("status", OrderStatus.valueOf(f.getStatus().toUpperCase()));
        }
        if (f.getKeyword() != null) {
            q.setParameter("kw", "%" + f.getKeyword().toLowerCase() + "%");
            Integer id = keywordAsId(f);
            if (id != null) q.setParameter("kwId", id);
        }
        if (f.getFrom() != null) q.setParameter("from", f.getFrom().atStartOfDay());
        if (f.getTo() != null) q.setParameter("toExclusive", f.getTo().plusDays(1).atStartOfDay()); // het ngay "to" = dau ngay ke tiep
    }

    private Integer keywordAsId(AdminOrderFilter f) {
        try {
            return Integer.valueOf(f.getKeyword().replace("#", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** So don theo tung trang thai (cho so tren cac tab), tinh tren TOAN BO don, khong theo bo loc. */
    public Map<OrderStatus, Long> countByStatus() {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
            for (Object[] row : em.createQuery("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status", Object[].class).getResultList()) {
                result.put((OrderStatus) row[0], (Long) row[1]);
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** Moi don trong danh sach co bao nhieu mat hang: 1 query gop (id don -> so dong), thay vi nap ca danh sach dong cua tung don. */
    public Map<Integer, Long> countItems(List<Integer> orderIds) {
        Map<Integer, Long> result = new HashMap<>();
        if (orderIds.isEmpty()) return result;
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            for (Object[] row : em.createQuery(
                    "SELECT i.order.id, COUNT(i) FROM OrderItem i WHERE i.order.id IN :ids GROUP BY i.order.id", Object[].class)
                    .setParameter("ids", orderIds).getResultList()) {
                result.put((Integer) row[0], (Long) row[1]);
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** Cac don gan day nhat cua 1 khach (moi nhat truoc), toi da `limit` don. Dung o trang chi tiet khach hang cua Admin. */
    public List<Order> findRecentByCustomer(Integer customerId, int limit) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT o FROM Order o WHERE o.customer.id = :cid ORDER BY o.id DESC", Order.class)
                    .setParameter("cid", customerId)
                    .setMaxResults(limit)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------ chi tiet

    /** 1 don kem day du: khach, cac dong hang + san pham, thanh toan, dia chi, voucher (JOIN FETCH vi EntityManager dong ngay sau khi tra ve). */
    public Order findDetail(Integer id) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<Order> result = em.createQuery(
                    "SELECT DISTINCT o FROM Order o JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product "
                            + "LEFT JOIN FETCH o.payment LEFT JOIN FETCH o.shippingAddress LEFT JOIN FETCH o.voucher WHERE o.id = :id", Order.class)
                    .setParameter("id", id)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------ doi trang thai

    /**
     * Doi trang thai don va moi viec di kem trong CUNG 1 transaction: hoac xong het hoac khong gi ca.
     *  - huy don / duyet hoan hang: cong lai kho cho tung dong hang (don huy con tra lai 1 luot voucher)
     *  - giao thanh cong: thanh toan COD chuyen sang SUCCESS; huy don: thanh toan dang cho chuyen sang FAILED
     *  - tao Notification cho khach
     * Luat "chuyen duoc hay khong" do Order.canMoveTo quyet dinh; Service kiem tra truoc de bao loi than thien.
     */
    public void applyStatusChange(Integer orderId, OrderStatus next, String notificationText) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Khoa dong don: 2 admin cung bam "Huy" 1 luc thi nguoi thu 2 phai doi, roi thay don da huy -> khong cong kho 2 lan.
            // Dung JPQL + setLockMode (khong dung em.find(..., lock)): voi find, Hibernate doc don bang query co JOIN truoc roi moi khoa tung bang,
            // nen luong cho van thay trang thai CU. Query don gian "WHERE id = ?" duoc them "FOR UPDATE" ngay trong cau SELECT.
            Order order = em.createQuery("SELECT o FROM Order o WHERE o.id = :id", Order.class)
                    .setParameter("id", orderId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult();
            order.updateStatus(next); // sai luat -> nem IllegalStateException -> rollback

            if (next == OrderStatus.CANCELLED || next == OrderStatus.RETURNED) {
                // Lấy ID trước; khoá sản phẩm theo cùng thứ tự với checkout.
                List<Integer> productIds = em.createQuery(
                        "SELECT DISTINCT i.product.id FROM OrderItem i "
                        + "WHERE i.order.id = :orderId", Integer.class)
                        .setParameter("orderId", orderId)
                        .getResultList();

                if (!productIds.isEmpty()) {
                    List<Product> products = em.createQuery(
                            "SELECT p FROM Product p WHERE p.id IN :ids "
                            + "ORDER BY p.id ASC", Product.class)
                            .setParameter("ids", productIds)
                            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                            .getResultList();

                    // EntityManager có thể giữ bản cũ: đọc lại SAU khi có khoá.
                    for (Product product : products) {
                        em.refresh(product);
                    }
                }

                for (OrderItem item : order.getItems()) {
                    item.getProduct().updateStock(item.getQuantity());
                }
            }

            if (next == OrderStatus.CANCELLED && order.getVoucher() != null) {
                // Khoá voucher sau sản phẩm, cùng thứ tự với checkout.
                Voucher voucher = em.createQuery(
                        "SELECT v FROM Voucher v WHERE v.id = :id", Voucher.class)
                        .setParameter("id", order.getVoucher().getId())
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getSingleResult();

                em.refresh(voucher);

                if (voucher.getQuantityUsed() > 0) {
                    voucher.setQuantityUsed(voucher.getQuantityUsed() - 1);
                }
            }
            Payment payment = order.getPayment();
            if (payment != null) {
                if (next == OrderStatus.COMPLETED && payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.SUCCESS); // COD: nhan tien luc giao hang xong
                    payment.setPaymentDate(LocalDateTime.now());
                } else if (next == OrderStatus.CANCELLED && payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                }
            }

            Notification n = new Notification();
            n.setUser(order.getCustomer());
            n.setMessage(notificationText);
            n.setRead(false);
            n.setCreatedAt(LocalDateTime.now());
            em.persist(n);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
