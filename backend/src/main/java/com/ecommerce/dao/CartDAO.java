package com.ecommerce.dao;

import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Customer;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.function.Consumer;

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

    public void changeCart(Integer customerId, Consumer<Cart> change) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            // Khoá chủ giỏ trước khi đọc giỏ.
            // Các request sửa giỏ cùng khách phải chờ nhau.
            Customer customer = em.createQuery(
                    "SELECT c FROM Customer c WHERE c.id = :id",
                    Customer.class)
                    .setParameter("id", customerId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult();

            List<Cart> result = em.createQuery(
                    "SELECT DISTINCT c FROM Cart c "
                    + "LEFT JOIN FETCH c.items i "
                    + "LEFT JOIN FETCH i.product "
                    + "WHERE c.customer.id = :customerId",
                    Cart.class)
                    .setParameter("customerId", customerId)
                    .getResultList();

            Cart cart;
            if (result.isEmpty()) {
                // Khoá chủ giỏ cũng ngăn hai request tạo hai giỏ cùng lúc.
                cart = new Cart();
                cart.setCustomer(customer);
                customer.setCart(cart);
                em.persist(cart);
            } else {
                cart = result.get(0);
            }

            // Service sẽ truyền phần kiểm tra và thay đổi giỏ vào đây.
            change.accept(cart);

            // Cart đang được EntityManager quản lý:
            // Hibernate lưu thay đổi khi commit, không cần merge lại.
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
