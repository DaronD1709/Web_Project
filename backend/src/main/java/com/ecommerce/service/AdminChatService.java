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

/** Nghiep vu CHAT phia Admin: xem cac cuoc tro chuyen, doc/gui tin, bat/tat che do nhan vien tiep quan. Khong dung WebSocket (xem docs/api-spec.md). */
public class AdminChatService {

    private static final int MAX_LENGTH = 1000;

    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final ChatService chatService = new ChatService();

    /** Moi khach co chat = 1 dong (tin cuoi cung), moi nhat truoc. mode: "all" | "human" | "ai"; keyword tim theo ten/email khach. */
    public List<Message> conversations(String mode, String keyword) {
        return messageDAO.findLatestPerCustomer(mode, keyword);
    }

    public Customer getCustomer(Integer customerId) {
        User user = customerId == null ? null : userDAO.findById(customerId);
        if (!(user instanceof Customer customer)) throw new BusinessException("Khách hàng không tồn tại.");
        return customer;
    }

    /** Tin nhan cua khach moi hon `afterId` (0 = tu dau). Khong chen loi chao cua chatbot nhu phia khach. */
    public List<Message> messagesAfter(Integer customerId, int afterId) {
        return messageDAO.findAfter(customerId, afterId);
    }

    /** Vai don gan day cua khach de Admin tra cuu khi dang chat. */
    public List<Order> recentOrders(Integer customerId) {
        return orderDAO.findRecentByCustomer(customerId, 3);
    }

    /** Admin gui tin. Gui tin nghia la nhan vien da vao cuoc nen tu chuyen sang che do NHAN VIEN (chatbot ngung tra loi). */
    public void send(Admin admin, Integer customerId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException("Vui lòng nhập nội dung tin nhắn.");
        if (text.length() > MAX_LENGTH) throw new BusinessException("Tin nhắn tối đa " + MAX_LENGTH + " ký tự.");
        Customer customer = getCustomer(customerId);

        Message m = new Message();
        m.setCustomer(customer);
        m.setSender(admin);
        m.setContent(text);
        m.setSentAt(LocalDateTime.now());
        messageDAO.save(m);

        chatService.setHandledByHuman(customerId, true);
    }

    /** Bat/tat che do nhan vien tiep quan thu cong. Tra ve cau thong bao. */
    public String setHuman(Integer customerId, boolean human) {
        getCustomer(customerId); // khong ton tai thi bao loi
        chatService.setHandledByHuman(customerId, human);
        return human ? "Bạn đã tiếp quản cuộc trò chuyện" : "Đã chuyển lại cho trợ lý AI";
    }
}
