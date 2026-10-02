package com.ecommerce.dao;

import com.ecommerce.entity.Conversation;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ConversationDAO extends AbstractDAO<Conversation, Integer> {

    public ConversationDAO() {
        super(Conversation.class);
    }

    /** Moi khach 1 cuoc tro chuyen (Customer 1-1 Conversation). Tra ve null neu chua co. */
    public Conversation findByCustomerId(Integer customerId) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<Conversation> result = em.createQuery(
                            "SELECT c FROM Conversation c WHERE c.customer.id = :customerId", Conversation.class)
                    .setParameter("customerId", customerId)
                    .setMaxResults(1)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }
}
