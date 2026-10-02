package com.ecommerce.dao;

import com.ecommerce.entity.Cart;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class CartDAO extends AbstractDAO<Cart, Integer> {

    public CartDAO() {
        super(Cart.class);
    }

    /**
     * Gio hang cua 1 khach, kem luon cac dong va san pham (JOIN FETCH) de JSP doc duoc sau khi EntityManager da dong
     * (collection lazy ma khong fetch o day se bi LazyInitializationException). Tra ve null neu chua co gio.
     */
    public Cart findByCustomerId(Integer customerId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<Cart> result = em.createQuery(
                            "SELECT DISTINCT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product "
                                    + "WHERE c.customer.id = :customerId", Cart.class)
                    .setParameter("customerId", customerId)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    /** So dong (san pham khac nhau) trong gio - hien o badge tren header. */
    public long countItems(Integer customerId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(i) FROM CartItem i WHERE i.cart.customer.id = :customerId", Long.class)
                    .setParameter("customerId", customerId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
