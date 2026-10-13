package com.ecommerce.dao;

import com.ecommerce.entity.Voucher;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

/** CRUD co ban dung AbstractDAO; them tim theo ma (kiem tra trung) va dem so don da dung voucher (chan xoa). */
public class VoucherDAO extends AbstractDAO<Voucher, Integer> {

    public VoucherDAO() {
        super(Voucher.class);
    }

    /** Moi nhat truoc. Voucher it nen lay het, khong phan trang. */
    public List<Voucher> findAllNewestFirst() {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT v FROM Voucher v ORDER BY v.id DESC", Voucher.class).getResultList();
        } finally {
            em.close();
        }
    }

    /** Tra ve null neu khong co voucher nao dung ma nay (so sanh khong phan biet hoa thuong). */
    public Voucher findByCode(String code) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<Voucher> result = em.createQuery("SELECT v FROM Voucher v WHERE UPPER(v.code) = :code", Voucher.class)
                    .setParameter("code", code.toUpperCase())
                    .setMaxResults(1)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    /** So don hang da ap dung voucher nay (> 0 thi khong duoc xoa voucher). */
    public long countOrders(Integer voucherId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(o) FROM Order o WHERE o.voucher.id = :id", Long.class)
                    .setParameter("id", voucherId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
