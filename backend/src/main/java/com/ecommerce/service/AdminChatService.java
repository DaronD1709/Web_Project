package com.ecommerce.service;

import com.ecommerce.dao.MessageDAO;
import com.ecommerce.dao.OrderDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Admin;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Message;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Nghiep vu CHAT phia Admin: xem cac cuoc tro chuyen, doc tin cua khach va tra loi bang tay. Khong dung WebSocket (xem docs/api-spec.md).
 * Khong co chatbot: moi tin cua khach chi duoc luu lai de nhan vien doc va tra loi (polling moi 3-5 giay).
 * Lop nay chi bao boc: truy van do MessageDAO lo.
 */
public class AdminChatService {

    private static final int MAX_LENGTH = 1000;

    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    /** Moi khach co chat = 1 dong (tin cuoi cung), moi nhat truoc. keyword tim theo ten/email khach (null = khong tim). */
    public List<Message> conversations(String keyword) {
        return messageDAO.findLatestPerCustomer(keyword);
    }

    // Tim khach theo id, id sai/thieu/khong phai Customer thi bao loi nghiep vu (Servlet bat va hien cho Admin).
    public Customer getCustomer(Integer customerId) {
        User user = customerId == null ? null : userDAO.findById(customerId);
        if (!(user instanceof Customer customer)) throw new BusinessException("Khách hàng không tồn tại.");
        return customer;
    }

    /** Tin nhan cua khach moi hon `afterId` (0 = tu dau). */
    public List<Message> messagesAfter(Integer customerId, int afterId) {
        return messageDAO.findAfter(customerId, afterId);
    }

    /** Vai don gan day cua khach de Admin tra cuu khi dang chat. */
    public List<Order> recentOrders(Integer customerId) {
        return orderDAO.findRecentByCustomer(customerId, 3);
    }

    /** Admin gui 1 tin tra loi khach (kiem tra noi dung roi luu). */
    public void send(Admin admin, Integer customerId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException("Vui lòng nhập nội dung tin nhắn.");
        if (text.length() > MAX_LENGTH) throw new BusinessException("Tin nhắn tối đa " + MAX_LENGTH + " ký tự.");
        Customer customer = getCustomer(customerId);

        // sender = Admin dang dang nhap; customer = chu cuoc chat. Khach se thay tin nay trong khung chat o lan polling ke tiep.
        Message m = new Message();
        m.setCustomer(customer);
        m.setSender(admin);
        m.setContent(text);
        m.setSentAt(LocalDateTime.now());
        messageDAO.save(m);
    }
}
