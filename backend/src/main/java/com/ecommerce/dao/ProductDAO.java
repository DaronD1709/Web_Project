package com.ecommerce.dao;

import com.ecommerce.entity.Product;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

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
}
