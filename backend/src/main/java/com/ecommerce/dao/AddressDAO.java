package com.ecommerce.dao;

import com.ecommerce.entity.Address;
import com.ecommerce.entity.Customer;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class AddressDAO extends AbstractDAO<Address, Integer> {

    public AddressDAO() {
        super(Address.class);
    }

    /**
     * Lấy danh sách địa chỉ của khách hàng.
     * Địa chỉ mặc định đứng trước, sau đó sắp xếp theo ID tăng dần.
     */
    public List<Address> findByCustomerId(Integer customerId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();

        try {
            return em.createQuery(
                            """
                            SELECT a
                            FROM Address a
                            WHERE a.customer.id = :customerId
                            ORDER BY a.isDefault DESC, a.id ASC
                            """,
                            Address.class)
                    .setParameter("customerId", customerId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Chỉ trả về địa chỉ nếu địa chỉ thuộc đúng khách hàng.
     */
    public Address findOwnedById(Integer addressId, Integer customerId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();

        try {
            return em.createQuery(
                            """
                            SELECT a
                            FROM Address a
                            WHERE a.id = :addressId
                              AND a.customer.id = :customerId
                            """,
                            Address.class)
                    .setParameter("addressId", addressId)
                    .setParameter("customerId", customerId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            em.close();
        }
    }

    /**
     * Tạo địa chỉ mới cho khách hàng.
     * Địa chỉ đầu tiên luôn tự động trở thành địa chỉ mặc định.
     */
    public Address createForCustomer(Integer customerId,
                                     Address address,
                                     boolean requestedDefault) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            long currentCount = em.createQuery(
                            """
                            SELECT COUNT(a)
                            FROM Address a
                            WHERE a.customer.id = :customerId
                            """,
                            Long.class)
                    .setParameter("customerId", customerId)
                    .getSingleResult();

            boolean makeDefault = requestedDefault || currentCount == 0;

            if (makeDefault) {
                clearDefault(em, customerId);
            }

            Customer customer = em.getReference(Customer.class, customerId);

            address.setCustomer(customer);
            address.setDefault(makeDefault);

            em.persist(address);
            tx.commit();

            return address;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }

            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Cập nhật một địa chỉ thuộc khách hàng.
     * Nếu requestedDefault=false thì giữ nguyên trạng thái mặc định hiện tại.
     */
    public Address updateOwned(Integer customerId,
                               Integer addressId,
                               Address values,
                               boolean requestedDefault) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Address address = findOwned(em, addressId, customerId);

            if (address == null) {
                tx.rollback();
                return null;
            }

            address.setRecipientName(values.getRecipientName());
            address.setPhone(values.getPhone());
            address.setStreet(values.getStreet());
            address.setCity(values.getCity());

            if (requestedDefault && !address.isDefault()) {
                clearDefault(em, customerId);
                address.setDefault(true);
            }

            tx.commit();
            return address;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }

            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Đặt một địa chỉ thuộc khách hàng làm địa chỉ mặc định.
     */
    public boolean setDefaultOwned(Integer customerId, Integer addressId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Address address = findOwned(em, addressId, customerId);

            if (address == null) {
                tx.rollback();
                return false;
            }

            if (address.isDefault()) {
                tx.commit();
                return true;
            }

            clearDefault(em, customerId);
            address.setDefault(true);

            tx.commit();
            return true;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }

            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Đếm số đơn hàng đang sử dụng địa chỉ.
     * Địa chỉ đã được sử dụng trong đơn hàng không được phép xóa.
     */
    public long countOrderReferences(Integer addressId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();

        try {
            return em.createQuery(
                            """
                            SELECT COUNT(o)
                            FROM Order o
                            WHERE o.shippingAddress.id = :addressId
                            """,
                            Long.class)
                    .setParameter("addressId", addressId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    /**
     * Xóa địa chỉ thuộc khách hàng.
     * Nếu xóa địa chỉ mặc định thì chọn địa chỉ còn lại đầu tiên làm mặc định.
     */
    public boolean deleteOwned(Integer customerId, Integer addressId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Address address = findOwned(em, addressId, customerId);

            if (address == null) {
                tx.rollback();
                return false;
            }

            long orderReferences = em.createQuery(
                            """
                            SELECT COUNT(o)
                            FROM Order o
                            WHERE o.shippingAddress.id = :addressId
                            """,
                            Long.class)
                    .setParameter("addressId", addressId)
                    .getSingleResult();

            if (orderReferences > 0) {
                tx.rollback();
                return false;
            }

            boolean deletedDefault = address.isDefault();

            em.remove(address);
            em.flush();

            if (deletedDefault) {
                List<Address> remaining = em.createQuery(
                                """
                                SELECT a
                                FROM Address a
                                WHERE a.customer.id = :customerId
                                ORDER BY a.id ASC
                                """,
                                Address.class)
                        .setParameter("customerId", customerId)
                        .setMaxResults(1)
                        .getResultList();

                if (!remaining.isEmpty()) {
                    remaining.get(0).setDefault(true);
                }
            }

            tx.commit();
            return true;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }

            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Tìm địa chỉ theo ID và ID khách hàng trong EntityManager hiện tại.
     */
    private Address findOwned(EntityManager em,
                              Integer addressId,
                              Integer customerId) {
        return em.createQuery(
                        """
                        SELECT a
                        FROM Address a
                        WHERE a.id = :addressId
                          AND a.customer.id = :customerId
                        """,
                        Address.class)
                .setParameter("addressId", addressId)
                .setParameter("customerId", customerId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Bỏ trạng thái mặc định của tất cả địa chỉ thuộc khách hàng.
     */
    private void clearDefault(EntityManager em, Integer customerId) {
        em.createQuery(
                        """
                        UPDATE Address a
                        SET a.isDefault = false
                        WHERE a.customer.id = :customerId
                        """)
                .setParameter("customerId", customerId)
                .executeUpdate();
    }
}