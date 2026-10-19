package com.ecommerce.dao;

import com.ecommerce.entity.AutoReply;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;

/** CRUD co ban dung AbstractDAO; them 2 cach lay danh sach theo thu tu uu tien (so nho truoc, bang nhau thi luat tao truoc). */
public class AutoReplyDAO extends AbstractDAO<AutoReply, Integer> {

    public AutoReplyDAO() {
        super(AutoReply.class);
    }

    /** Tat ca luat (ca dang tat) cho trang quan ly. ORDER BY priority roi id: so uu tien nho hien truoc, bang nhau thi luat tao truoc hien truoc. */
    public List<AutoReply> findAllOrdered() {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT r FROM AutoReply r ORDER BY r.priority ASC, r.id ASC", AutoReply.class).getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Chi cac luat dang BAT, chatbot dung khi tra loi khach. Goi LAI tu DB moi lan khach gui tin (1 query),
     * nen Admin sua/bat/tat luat la co hieu luc ngay, khong can khoi dong lai ung dung.
     */
    public List<AutoReply> findActiveOrdered() {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT r FROM AutoReply r WHERE r.active = true ORDER BY r.priority ASC, r.id ASC", AutoReply.class).getResultList();
        } finally {
            em.close();
        }
    }
}
