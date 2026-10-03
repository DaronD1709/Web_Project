package com.ecommerce.dao;

import com.ecommerce.entity.Message;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class MessageDAO extends AbstractDAO<Message, Integer> {

    public MessageDAO() {
        super(Message.class);
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
