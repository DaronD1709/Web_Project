package com.ecommerce.dao;

import com.ecommerce.dto.TopProduct;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cac truy van THONG KE cho trang Tong quan / Thong ke doanh thu (chi doc, khong ghi). Don duoc tinh vao doanh thu: dang xu ly + hoan tat
 * (PENDING, CONFIRMED, SHIPPING, COMPLETED); khong tinh don huy va don hoan hang (RETURN_REQUESTED, RETURNED).
 */
public class StatsDAO {

    public static final List<OrderStatus> COUNTED = List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.COMPLETED);

    /** (thoi diem dat, tong tien) cua cac don tinh doanh thu trong [from, to). Service gom theo ngay bang Java (so don it, don gian hon GROUP BY theo ngay). */
    public List<Object[]> revenueRows(LocalDateTime from, LocalDateTime to) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT o.orderDate, o.totalAmount FROM Order o "
                            + "WHERE o.orderDate >= :from AND o.orderDate < :to AND o.status IN :counted", Object[].class)
                    .setParameter("from", from).setParameter("to", to).setParameter("counted", COUNTED)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** So don trong [from, to) co trang thai thuoc `statuses` (null = moi trang thai). */
    public long countOrders(LocalDateTime from, LocalDateTime to, List<OrderStatus> statuses) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            var q = em.createQuery("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :from AND o.orderDate < :to"
                    + (statuses == null ? "" : " AND o.status IN :statuses"), Long.class)
                    .setParameter("from", from).setParameter("to", to);
            if (statuses != null) q.setParameter("statuses", statuses);
            return q.getSingleResult();
        } finally {
            em.close();
        }
    }

    /** Doanh thu theo danh muc (tong thanh tien cac dong hang), cao xuong thap. Thanh tien dung gia luc DAT (priceAtOrder), khong phai gia hien tai. */
    public Map<String, Double> revenueByCategory(LocalDateTime from, LocalDateTime to) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Map<String, Double> result = new LinkedHashMap<>();
            for (Object[] row : em.createQuery("SELECT p.category.name, SUM(i.priceAtOrder * i.quantity) FROM OrderItem i JOIN i.product p JOIN i.order o "
                            + "WHERE o.orderDate >= :from AND o.orderDate < :to AND o.status IN :counted "
                            + "GROUP BY p.category.name ORDER BY SUM(i.priceAtOrder * i.quantity) DESC", Object[].class)
                    .setParameter("from", from).setParameter("to", to).setParameter("counted", COUNTED).getResultList()) {
                result.put((String) row[0], ((Number) row[1]).doubleValue());
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** San pham ban chay nhat theo so luong, toi da `limit` dong. */
    public List<TopProduct> topProducts(LocalDateTime from, LocalDateTime to, int limit) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<TopProduct> result = new ArrayList<>();
            for (Object[] row : em.createQuery("SELECT p.id, p.name, p.category.name, SUM(i.quantity), SUM(i.priceAtOrder * i.quantity) "
                            + "FROM OrderItem i JOIN i.product p JOIN i.order o "
                            + "WHERE o.orderDate >= :from AND o.orderDate < :to AND o.status IN :counted "
                            + "GROUP BY p.id, p.name, p.category.name ORDER BY SUM(i.quantity) DESC, p.id", Object[].class)
                    .setParameter("from", from).setParameter("to", to).setParameter("counted", COUNTED)
                    .setMaxResults(limit).getResultList()) {
                result.add(new TopProduct((Integer) row[0], (String) row[1], (String) row[2], ((Number) row[3]).longValue(), ((Number) row[4]).doubleValue()));
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** San pham co ton kho <= threshold, it nhat truoc, toi da `limit` dong. */
    public List<Product> lowStock(int threshold, int limit) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT p FROM Product p WHERE p.stockQuantity <= :t ORDER BY p.stockQuantity ASC, p.id", Product.class)
                    .setParameter("t", threshold).setMaxResults(limit).getResultList();
        } finally {
            em.close();
        }
    }

    public long countLowStock(int threshold) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity <= :t", Long.class).setParameter("t", threshold).getSingleResult();
        } finally {
            em.close();
        }
    }
}
