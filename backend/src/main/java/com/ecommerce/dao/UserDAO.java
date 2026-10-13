package com.ecommerce.dao;

import com.ecommerce.entity.User;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

// Dung chung cho Customer/Admin (cung bang "users"). Dung o dang nhap, dang ky (check trung email).
public class UserDAO extends AbstractDAO<User, Integer> {

    public UserDAO() {
        super(User.class);
    }

    /** Tra ve null neu khong co user nao dung email nay (giong findById). */
    public User findByEmail(String email) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<User> result = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .setMaxResults(1)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    /** Tim user theo ban BAM cua token quen mat khau (khong bao gio tim bang token goc). */
    public User findByResetTokenHash(String tokenHash) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<User> result = em.createQuery("SELECT u FROM User u WHERE u.resetTokenHash = :hash", User.class)
                    .setParameter("hash", tokenHash)
                    .setMaxResults(1)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    /** Chi sua thong tin ho so; khong merge Customer detached de tranh cascade sang gio/dia chi. */
    public boolean updateProfile(Integer customerId, String fullName, String phone) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int updated = em.createQuery("UPDATE Customer c SET c.fullName = :name, c.phone = :phone WHERE c.id = :id")
                    .setParameter("name", fullName).setParameter("phone", phone)
                    .setParameter("id", customerId).executeUpdate();
            tx.commit();
            return updated == 1;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** Chi doi neu hash van la hash da kiem tra; vo hieu token quen mat khau cu trong cung transaction. */
    public boolean changePassword(Integer customerId, String expectedHash, String newHash) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            int updated = em.createQuery("UPDATE Customer c SET c.passwordHash = :newHash, "
                    + "c.resetTokenHash = NULL, c.resetTokenExpiry = NULL "
                    + "WHERE c.id = :id AND c.passwordHash = :expectedHash")
                    .setParameter("newHash", newHash).setParameter("id", customerId)
                    .setParameter("expectedHash", expectedHash).executeUpdate();
            tx.commit();
            return updated == 1;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
