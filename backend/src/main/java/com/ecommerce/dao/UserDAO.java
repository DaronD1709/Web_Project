package com.ecommerce.dao;

import com.ecommerce.entity.User;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

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
}
