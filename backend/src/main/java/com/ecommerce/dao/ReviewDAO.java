package com.ecommerce.dao;

import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Review;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

/** Truy van danh gia san pham. save/update dung tu AbstractDAO. */
public class ReviewDAO extends AbstractDAO<Review, Integer> {

    // Don duoc coi la "da mua": da giao xong hoac da tra hang (rule trong CLAUDE.md muc 5)
    private static final List<OrderStatus> PURCHASED = List.of(OrderStatus.COMPLETED, OrderStatus.RETURNED);

    public ReviewDAO() {
        super(Review.class);
    }

    /** Danh gia cua 1 san pham, moi nhat truoc, toi da `limit`. Khach la @ManyToOne eager nen JSP doc duoc ten sau khi EM dong. */
    public List<Review> findByProduct(Integer productId, int limit) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT r FROM Review r JOIN FETCH r.customer WHERE r.product.id = :pid ORDER BY r.createdAt DESC, r.id DESC", Review.class)
                    .setParameter("pid", productId).setMaxResults(limit).getResultList();
        } finally {
            em.close();
        }
    }

    /** {so danh gia, diem trung binh} cua san pham (chua co danh gia: {0, 0.0}). */
    public double[] summary(Integer productId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Object[] row = em.createQuery("SELECT COUNT(r), AVG(r.rating) FROM Review r WHERE r.product.id = :pid", Object[].class)
                    .setParameter("pid", productId).getSingleResult();
            long count = ((Number) row[0]).longValue();
            return new double[]{count, count == 0 ? 0.0 : ((Number) row[1]).doubleValue()};
        } finally {
            em.close();
        }
    }

    /** Danh gia san pham nay cua khach (null neu chua danh gia). */
    public Review findByCustomerAndProduct(Integer customerId, Integer productId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<Review> list = em.createQuery("SELECT r FROM Review r JOIN FETCH r.customer WHERE r.customer.id = :cid AND r.product.id = :pid", Review.class)
                    .setParameter("cid", customerId).setParameter("pid", productId).getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }

    /** Khach co it nhat 1 don COMPLETED/RETURNED chua san pham nay khong. */
    public boolean hasPurchased(Integer customerId, Integer productId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Long n = em.createQuery("SELECT COUNT(o) FROM Order o JOIN o.items i WHERE o.customer.id = :cid AND i.product.id = :pid AND o.status IN :st", Long.class)
                    .setParameter("cid", customerId).setParameter("pid", productId).setParameter("st", PURCHASED).getSingleResult();
            return n > 0;
        } finally {
            em.close();
        }
    }
}
