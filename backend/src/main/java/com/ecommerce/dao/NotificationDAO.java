package com.ecommerce.dao;

import com.ecommerce.dto.PageResult;
import com.ecommerce.entity.Notification;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class NotificationDAO extends AbstractDAO<Notification, Integer> {
    public NotificationDAO() { super(Notification.class); }

    public PageResult<Notification> findOwn(Integer userId, boolean unread, int page) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            String where = " WHERE n.user.id = :uid" + (unread ? " AND n.isRead = false" : "");
            long total = em.createQuery("SELECT COUNT(n) FROM Notification n" + where, Long.class)
                    .setParameter("uid", userId).getSingleResult();
            int current = (int) Math.min(page, Math.max(1L, (total + 19) / 20));
            var items = em.createQuery("SELECT n FROM Notification n" + where + " ORDER BY n.id DESC", Notification.class)
                    .setParameter("uid", userId)
                    .setFirstResult(Math.toIntExact(((long) current - 1) * 20)).setMaxResults(20)
                    .getResultList();
            return new PageResult<>(items, total, current, 20);
        } finally {
            em.close();
        }
    }

    public long countUnread(Integer userId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :uid AND n.isRead = false", Long.class)
                    .setParameter("uid", userId).getSingleResult();
        } finally {
            em.close();
        }
    }

    /** WHERE luôn giới hạn đúng chủ; đọc lại nhiều lần không tạo thêm thay đổi. */
    public void markRead(Integer userId, Integer notificationId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            String where = " WHERE n.user.id = :uid AND n.isRead = false"
                    + (notificationId == null ? "" : " AND n.id = :id");
            var query = em.createQuery("UPDATE Notification n SET n.isRead = true" + where)
                    .setParameter("uid", userId);
            if (notificationId != null) query.setParameter("id", notificationId);
            query.executeUpdate();
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
