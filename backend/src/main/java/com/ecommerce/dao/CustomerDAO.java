package com.ecommerce.dao;

import com.ecommerce.dto.AdminCustomerFilter;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Truy van khach hang cho Admin: loc/phan trang, thong ke so don va tong chi tieu. */
public class CustomerDAO extends AbstractDAO<Customer, Integer> {

    public CustomerDAO() {
        super(Customer.class);
    }

    /** 1 trang khach hang khop bo loc, tham gia moi nhat truoc. */
    public List<Customer> search(AdminCustomerFilter f) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Query query = em.createQuery("SELECT c FROM Customer c" + buildWhere(f) + " ORDER BY c.id DESC", Customer.class);
            setParams(query, f);
            query.setFirstResult(f.getOffset());
            query.setMaxResults(f.getPageSize());
            @SuppressWarnings("unchecked")
            List<Customer> result = query.getResultList();
            return result;
        } finally {
            em.close();
        }
    }

    public long countSearch(AdminCustomerFilter f) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(c) FROM Customer c" + buildWhere(f), Long.class);
            setParams(query, f);
            return (Long) query.getSingleResult();
        } finally {
            em.close();
        }
    }

    // Chi noi DIEU KIEN CO DINH vao chuoi; tu khoa nguoi dung nhap luon qua setParameter (chong JPQL injection).
    private String buildWhere(AdminCustomerFilter f) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        if (f.getKeyword() != null) {
            w.append(" AND (LOWER(c.fullName) LIKE :kw OR LOWER(c.email) LIKE :kw OR LOWER(c.phone) LIKE :kw)");
        }
        // Cot is_active co the NULL o dong cu -> coi NULL la "dang hoat dong"
        if ("active".equals(f.getStatus())) w.append(" AND (c.active = true OR c.active IS NULL)");
        if ("locked".equals(f.getStatus())) w.append(" AND c.active = false");
        return w.toString();
    }

    private void setParams(Query q, AdminCustomerFilter f) {
        if (f.getKeyword() != null) q.setParameter("kw", "%" + f.getKeyword().toLowerCase() + "%");
    }

    /** So tai khoan dang hoat dong / da khoa (cho dong tong tren trang). Khoa "all", "locked". */
    public Map<String, Long> countLocked() {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Map<String, Long> result = new HashMap<>();
            result.put("all", em.createQuery("SELECT COUNT(c) FROM Customer c", Long.class).getSingleResult());
            result.put("locked", em.createQuery("SELECT COUNT(c) FROM Customer c WHERE c.active = false", Long.class).getSingleResult());
            return result;
        } finally {
            em.close();
        }
    }

    /**
     * Thong ke cua cac khach da cho: id khach -> {so don, tong chi tieu}. Tong chi tieu KHONG tinh don da huy.
     * 1 query gop thay vi dem tung khach (tranh N+1); khach chua co don khong co trong ket qua (JSP coi la 0).
     */
    public Map<Integer, double[]> orderStats(List<Integer> customerIds) {
        Map<Integer, double[]> result = new HashMap<>();
        if (customerIds.isEmpty()) return result;
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<Object[]> rows = em.createQuery(
                    "SELECT o.customer.id, COUNT(o), COALESCE(SUM(CASE WHEN o.status <> :cancelled THEN o.totalAmount ELSE 0 END), 0) "
                            + "FROM Order o WHERE o.customer.id IN :ids GROUP BY o.customer.id", Object[].class)
                    .setParameter("cancelled", OrderStatus.CANCELLED)
                    .setParameter("ids", customerIds)
                    .getResultList();
            for (Object[] r : rows) {
                result.put((Integer) r[0], new double[]{((Long) r[1]).doubleValue(), ((Number) r[2]).doubleValue()});
            }
            return result;
        } finally {
            em.close();
        }
    }
}
