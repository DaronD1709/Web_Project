package com.ecommerce.dao;

import com.ecommerce.entity.Category;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CRUD co ban dung AbstractDAO; them cac truy van cho trang quan ly danh muc cua Admin.
public class CategoryDAO extends AbstractDAO<Category, Integer> {

    public CategoryDAO() {
        super(Category.class);
    }

    /** Tat ca danh muc theo thu tu tao (id tang dan). */
    public List<Category> findAllOrdered() {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT c FROM Category c ORDER BY c.id", Category.class).getResultList();
        } finally {
            em.close();
        }
    }

    /** id danh muc -> so san pham trong danh muc (1 query gop, tranh dem tung danh muc); danh muc rong khong co trong ket qua (JSP coi la 0). */
    public Map<Integer, Long> countProducts() {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Map<Integer, Long> result = new HashMap<>();
            for (Object[] row : em.createQuery("SELECT p.category.id, COUNT(p) FROM Product p GROUP BY p.category.id", Object[].class).getResultList()) {
                result.put((Integer) row[0], (Long) row[1]);
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** Tim theo ten, khong phan biet hoa thuong (kiem tra trung ten); null neu khong co. */
    public Category findByName(String name) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<Category> result = em.createQuery("SELECT c FROM Category c WHERE LOWER(c.name) = :name", Category.class)
                    .setParameter("name", name.toLowerCase())
                    .setMaxResults(1)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    /** So san pham thuoc 1 danh muc (> 0 thi khong duoc xoa danh muc). */
    public long countProducts(Integer categoryId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(p) FROM Product p WHERE p.category.id = :id", Long.class)
                    .setParameter("id", categoryId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
