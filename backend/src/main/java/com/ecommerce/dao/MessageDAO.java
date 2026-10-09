package com.ecommerce.dao;

import com.ecommerce.entity.Message;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class MessageDAO extends AbstractDAO<Message, Integer> {

    public MessageDAO() {
        super(Message.class);
    }

    /**
     * Moi khach co chat = 1 dong: TIN CUOI CUNG cua cuoc tro chuyen (moi nhat truoc). mode: "all" | "human" (nhan vien dang tiep quan) | "ai".
     * keyword: tim theo ten/email khach (null = khong tim). JOIN FETCH khach + nguoi gui de JSP doc duoc sau khi EntityManager dong.
     */
    public List<Message> findLatestPerCustomer(String mode, String keyword) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT m FROM Message m JOIN FETCH m.customer c JOIN FETCH m.sender "
                    + "WHERE m.id IN (SELECT MAX(m2.id) FROM Message m2 GROUP BY m2.customer.id)");
            if ("human".equals(mode)) jpql.append(" AND c.handledByHuman = true");
            if ("ai".equals(mode)) jpql.append(" AND (c.handledByHuman = false OR c.handledByHuman IS NULL)");
            if (keyword != null) jpql.append(" AND (LOWER(c.fullName) LIKE :kw OR LOWER(c.email) LIKE :kw)");
            jpql.append(" ORDER BY m.sentAt DESC, m.id DESC"); // hoi thoai co tin moi nhat len dau
            var query = em.createQuery(jpql.toString(), Message.class);
            if (keyword != null) query.setParameter("kw", "%" + keyword.toLowerCase() + "%");
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /** Tin nhan moi hon `afterId` (id lon hon), cu truoc moi sau; JOIN FETCH sender de JSP doc ten/vai tro sau khi EM dong. */
    public List<Message> findAfter(Integer customerId, int afterId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM Message m JOIN FETCH m.sender "
                                    + "WHERE m.customer.id = :cid AND m.id > :afterId ORDER BY m.id", Message.class)
                    .setParameter("cid", customerId)
                    .setParameter("afterId", afterId)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
