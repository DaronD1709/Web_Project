package com.ecommerce.dao;

import com.ecommerce.dto.ProductFilter;
import com.ecommerce.entity.Product;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;

import java.util.List;
import java.util.Locale;

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
    public List<Product> findRelated(Integer categoryId, Integer excludedProductId, int limit) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery(
                        """
                        SELECT p
                        FROM Product p
                        WHERE p.category.id = :categoryId
                          AND p.id <> :excludedProductId
                        ORDER BY p.id DESC
                        """,
                        Product.class)
                .setParameter("categoryId", categoryId)
                .setParameter("excludedProductId", excludedProductId)
                .setMaxResults(limit)
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

    /** So dong chi tiet don hang dang tham chieu san pham nay (> 0 thi khong duoc xoa san pham). */
    public long countOrderItems(Integer productId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(i) FROM OrderItem i WHERE i.product.id = :id", Long.class)
                    .setParameter("id", productId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    /**
     * Xoa san pham trong 1 transaction: go no khoi cac gio hang truoc (cart_items co khoa ngoai toi products),
     * roi xoa san pham (danh gia xoa theo nho cascade). Don hang da dat thi Service da chan tu truoc.
     */
    public void deleteWithCartItems(Integer productId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM CartItem c WHERE c.product.id = :id").setParameter("id", productId).executeUpdate();
            Product p = em.find(Product.class, productId);
            if (p != null) em.remove(p);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
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
        if (f.getKeyword() != null) {
            // Khong phan biet hoa thuong VA dau tieng Viet: "thuoc tru sau" van ra "Thuốc trừ sâu". translate() la ham san co cua Postgres
            // (doi tung ky tu co dau -> khong dau), 2 chuoi hang so ben duoi khong chua du lieu nguoi dung nen noi vao JPQL la an toan.
            where.append(" AND CAST(function('translate', LOWER(p.name), '" + ACCENTED + "', '" + PLAIN + "') AS String) LIKE :keyword ESCAPE '!'");
        }
        if (f.getMinPrice() != null) where.append(" AND p.price >= :minPrice");
        if (f.getMaxPrice() != null) where.append(" AND p.price <= :maxPrice");
        if (f.isInStockOnly()) where.append(" AND p.stockQuantity > 0");
        // Loc ton kho cua Admin: cac gia tri da duoc ProductFilter.setStock gioi han, hang so noi vao chuoi la an toan.
        switch (f.getStock()) {
            case "in" -> where.append(" AND p.stockQuantity > 0");
            case "low" -> where.append(" AND p.stockQuantity > 0 AND p.stockQuantity <= ").append(ProductFilter.LOW_STOCK_LIMIT);
            case "out" -> where.append(" AND p.stockQuantity = 0");
            default -> { }
        }
        return where.toString();
    }

    // ORDER BY khong the dung tham so -> chon trong danh sach co dinh, khong dung chuoi tu nguoi dung.
    private String buildOrderBy(ProductFilter f) {
        return switch (f.getSort()) {
            case "asc" -> " ORDER BY p.price ASC";
            case "desc" -> " ORDER BY p.price DESC";
            case "name" -> " ORDER BY LOWER(p.name) ASC";
            case "stock" -> " ORDER BY p.stockQuantity ASC, p.id DESC"; // ton kho thap truoc
            default -> " ORDER BY p.id DESC"; // moi nhat
        };
    }

    private void setParams(Query query, ProductFilter f) {
        if (f.getCategoryId() != null) {
            query.setParameter("categoryId", f.getCategoryId());
        }
        if (f.getKeyword() != null) {
            String keyword = escapeLike(stripAccents(f.getKeyword()));
            query.setParameter("keyword", "%" + keyword + "%");
        }
        if (f.getMinPrice() != null) {
            query.setParameter("minPrice", f.getMinPrice());
        }
        if (f.getMaxPrice() != null) {
            query.setParameter("maxPrice", f.getMaxPrice());
        }
    }

    // Bang doi dau: ACCENTED[i] -> PLAIN[i]. Gom chu thuong co dau + chu HOA co dau (phong khi LOWER cua DB khong doi duoc chu hoa co dau).
    private static final String ACCENTED;
    private static final String PLAIN;
    static {
        String[] groups = {"aàáạảãâầấậẩẫăằắặẳẵ", "eèéẹẻẽêềếệểễ", "iìíịỉĩ", "oòóọỏõôồốộổỗơờớợởỡ", "uùúụủũưừứựửữ", "yỳýỵỷỹ", "dđ"};
        StringBuilder from = new StringBuilder(), to = new StringBuilder();
        for (String g : groups) {
            char plain = g.charAt(0);
            for (char c : g.substring(1).toCharArray()) {
                from.append(c).append(Character.toUpperCase(c));
                to.append(plain).append(plain);
            }
        }
        ACCENTED = from.toString();
        PLAIN = to.toString();
    }

    /** "Thuốc Trừ Sâu" -> "thuoc tru sau" (chu thuong, bo dau tieng Viet; dong bo voi translate() o buildWhere). */
    static String stripAccents(String value) {
        String lower = value.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return java.text.Normalizer.normalize(lower, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private String escapeLike(String value) {
        return value
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    }
}
