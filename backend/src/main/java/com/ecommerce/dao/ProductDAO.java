package com.ecommerce.dao;

import com.ecommerce.dto.ProductFilter;
import com.ecommerce.entity.Product;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

// Vi du DAO cu the: chi them nhung query dac thu, CRUD co ban da co san tu AbstractDAO.
public class ProductDAO extends AbstractDAO<Product, Integer> {

    public ProductDAO() {
        super(Product.class);
    }

    public List<Product> findByCategoryId(Integer categoryId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Product p WHERE p.category.id = :categoryId", Product.class)
                    .setParameter("categoryId", categoryId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Cac san pham moi nhat (id lon nhat truoc) - dung cho muc "noi bat" o trang chu. */
    public List<Product> findLatest(int limit) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT p FROM Product p ORDER BY p.id DESC", Product.class)
                    .setMaxResults(limit)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Tim kiem + loc + sap xep + phan trang cho trang /products. */
    public List<Product> search(ProductFilter f) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            String jpql = "SELECT p FROM Product p" + buildWhere(f) + buildOrderBy(f);
            Query query = em.createQuery(jpql, Product.class);
            setParams(query, f);
            query.setFirstResult(f.getOffset());
            query.setMaxResults(f.getPageSize());
            @SuppressWarnings("unchecked")
            List<Product> result = query.getResultList();
            return result;
        } finally {
            em.close();
        }
    }

    /** Tong so san pham khop bo loc (khong phan trang) - de tinh so trang. */
    public long countSearch(ProductFilter f) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(p) FROM Product p" + buildWhere(f), Long.class);
            setParams(query, f);
            return (Long) query.getSingleResult();
        } finally {
            em.close();
        }
    }

    // Chi noi cac DIEU KIEN CO DINH vao chuoi JPQL; gia tri nguoi dung nhap luon di qua setParameter
    // (khong bao gio noi truc tiep vao chuoi) -> chong SQL/JPQL injection.
    private String buildWhere(ProductFilter f) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (f.getCategoryId() != null) where.append(" AND p.category.id = :categoryId");
        if (f.getKeyword() != null) where.append(" AND LOWER(p.name) LIKE :keyword");
        if (f.getMinPrice() != null) where.append(" AND p.price >= :minPrice");
        if (f.getMaxPrice() != null) where.append(" AND p.price <= :maxPrice");
        if (f.isInStockOnly()) where.append(" AND p.stockQuantity > 0");
        return where.toString();
    }

    // ORDER BY khong the dung tham so -> chon trong danh sach co dinh, khong dung chuoi tu nguoi dung.
    private String buildOrderBy(ProductFilter f) {
        return switch (f.getSort()) {
            case "asc" -> " ORDER BY p.price ASC";
            case "desc" -> " ORDER BY p.price DESC";
            default -> " ORDER BY p.id DESC"; // moi nhat
        };
    }

    private void setParams(Query query, ProductFilter f) {
        if (f.getCategoryId() != null) query.setParameter("categoryId", f.getCategoryId());
        if (f.getKeyword() != null) query.setParameter("keyword", "%" + f.getKeyword().toLowerCase() + "%");
        if (f.getMinPrice() != null) query.setParameter("minPrice", f.getMinPrice());
        if (f.getMaxPrice() != null) query.setParameter("maxPrice", f.getMaxPrice());
    }
}
