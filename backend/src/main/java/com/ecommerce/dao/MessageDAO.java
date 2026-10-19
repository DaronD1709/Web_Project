package com.ecommerce.dao;

import com.ecommerce.entity.Message;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Truy van tin nhan chat. Co 2 truy van dac thu, ca hai deu phuc vu co che POLLING cua htmx (khong dung WebSocket):
 *   - findLatestPerCustomer: danh sach hoi thoai ben Admin (cot trai), duoc hoi lai moi 5 giay;
 *   - findAfter: chi nhung tin MOI hon id da biet, duoc hoi lai moi 3 giay o khung chat (ca khach lan Admin).
 * Cac thao tac CRUD thong thuong (luu tin moi) dung method save() ke thua tu AbstractDAO.
 */
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
            // Truy van con "SELECT MAX(id) ... GROUP BY customer" = id tin CUOI CUNG cua tung khach; truy van ngoai lay ra dung cac tin do.
            // Cach nay gon hon nap het tin nhan roi tu loc, va chi tra 1 dong cho moi khach.
            StringBuilder jpql = new StringBuilder("SELECT m FROM Message m JOIN FETCH m.customer c JOIN FETCH m.sender "
                    + "WHERE m.id IN (SELECT MAX(m2.id) FROM Message m2 GROUP BY m2.customer.id)");
            // mode chi nhan 3 gia tri co dinh nen noi chuoi nay an toan; "IS NULL" de cac khach cu (cot moi them, chua co gia tri) van tinh la chatbot.
            if ("human".equals(mode)) jpql.append(" AND c.handledByHuman = true");
            if ("ai".equals(mode)) jpql.append(" AND (c.handledByHuman = false OR c.handledByHuman IS NULL)");
            // Tu khoa di qua tham so :kw (setParameter ben duoi), khong noi thang vao chuoi -> chong JPQL injection.
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
    // Day la truy van cua co che polling: trinh duyet nho "id tin cuoi da hien" (afterId) va chi xin nhung tin co id lon hon.
    // Khong co tin moi -> tra danh sach rong -> khung chat khong doi gi, nhe cho server. WHERE customer_id dam bao khach chi doc tin cua chinh minh.
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
