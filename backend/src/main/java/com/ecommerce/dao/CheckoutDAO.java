package com.ecommerce.dao;

import com.ecommerce.entity.Address;
import com.ecommerce.util.JPAUtil;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Voucher;
import com.ecommerce.entity.Notification;
import com.ecommerce.entity.Order;


import java.util.Locale;
import java.time.LocalDateTime;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.EntityManager;

import java.util.List;

/** Truy xuất dữ liệu phục vụ luồng đặt hàng. */
public class CheckoutDAO {

    public List<Address> findAddressesByCustomer(Integer customerId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();

        try {
            // Chỉ lấy địa chỉ của khách được Service truyền xuống.
            // Địa chỉ mặc định đứng trước, sau đó sắp theo ID.
            return em.createQuery(
                    "SELECT a FROM Address a "
                    + "WHERE a.customer.id = :customerId "
                    + "ORDER BY a.isDefault DESC, a.id ASC",
                    Address.class)
                    .setParameter("customerId", customerId)
                    .getResultList();
        } finally {
            // Luôn trả kết nối, kể cả khi truy vấn gặp lỗi.
            em.close();
        }
    }

    private Customer lockCustomer(EntityManager em, Integer customerId) {
        // Khoá trước khi đọc giỏ: dùng cùng dòng Customer
        // mà CartDAO.changeCart đang khoá.
        List<Customer> result = em.createQuery(
                "SELECT c FROM Customer c WHERE c.id = :customerId",
                Customer.class)
                .setParameter("customerId", customerId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE) //giữ khoá đến khi transaction commit hoặc rollback.
                .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    private Address lockAddress(EntityManager em,
                                Integer addressId,
                                Integer customerId) {
        // Chỉ lấy và khoá địa chỉ thuộc đúng khách hàng.
        List<Address> result = em.createQuery(
                "SELECT a FROM Address a "
                + "WHERE a.id = :addressId "
                + "AND a.customer.id = :customerId",
                Address.class)
                .setParameter("addressId", addressId)
                .setParameter("customerId", customerId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    private void lockProducts(EntityManager em, Integer customerId) {
        // Đọc ID trước, chưa tải các entity Product vào bộ nhớ.
        List<Integer> productIds = em.createQuery(
                "SELECT DISTINCT i.product.id FROM CartItem i "
                + "WHERE i.cart.customer.id = :customerId",
                Integer.class)
                .setParameter("customerId", customerId)
                .getResultList();

        if (productIds.isEmpty()) {
            return;
        }

        // Các checkout lấy khoá sản phẩm theo cùng thứ tự ID.
        em.createQuery(
                "SELECT p FROM Product p "
                + "WHERE p.id IN :productIds ORDER BY p.id ASC",
                Product.class)
                .setParameter("productIds", productIds)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    private Cart findCart(EntityManager em, Integer customerId) {
        // Gọi sau lockProducts để kiểm tra tồn kho từ dữ liệu đã khoá.
        List<Cart> result = em.createQuery(
                "SELECT DISTINCT c FROM Cart c "
                + "LEFT JOIN FETCH c.items i "
                + "LEFT JOIN FETCH i.product "
                + "WHERE c.customer.id = :customerId",
                Cart.class)
                .setParameter("customerId", customerId)
                .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    private Voucher lockVoucher(EntityManager em, String code) {
        // Voucher không bắt buộc khi đặt hàng.
        if (code == null || code.isBlank()) {
            return null;
        }

        List<Voucher> result = em.createQuery(
                "SELECT v FROM Voucher v WHERE UPPER(v.code) = :code",
                Voucher.class)
                .setParameter("code", code.trim().toUpperCase(Locale.ROOT))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    @FunctionalInterface
    public interface OrderBuilder {
        Order build(Customer customer, Cart cart,
                    Address address, Voucher voucher);
    }

    public Order createOrder(Integer customerId,
                            Integer addressId,
                            String voucherCode,
                            String notificationText,
                            OrderBuilder builder) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            // Lấy dữ liệu trong cùng transaction với các khoá đã chuẩn bị.
            Customer customer = lockCustomer(em, customerId);
            Address address = lockAddress(em, addressId, customerId);

            lockProducts(em, customerId);
            Cart cart = findCart(em, customerId);
            Voucher voucher = lockVoucher(em, voucherCode);

            // Service truyền vào phần kiểm tra nghiệp vụ và dựng đơn.
            // Phần này chạy ngay tại đây, trước khi transaction commit.
            Order order = builder.build(customer, cart, address, voucher);

            // Cascade của Order lưu luôn OrderItem và Payment.
            em.persist(order);

            Notification notification = new Notification();
            notification.setUser(order.getCustomer());
            notification.setMessage(notificationText);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            em.persist(notification);

            // Lưu cả đơn và thay đổi tồn kho/voucher/giỏ trong transaction.
            tx.commit();
            return order;
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