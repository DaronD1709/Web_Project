package com.ecommerce.service;

import com.ecommerce.dao.ConversationDAO;
import com.ecommerce.dao.MessageDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Conversation;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Message;
import com.ecommerce.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat giua khach va shop. Moi khach co 1 Conversation; Message.sender la User (Customer / Admin / AIBot).
 * Hien tai: khach gui va xem tin (polling bang htmx). Tra loi tu dong cua AI va phia Admin lam o nhanh feat/chat-with-shop.
 */
public class ChatService {

    public static final String BOT_EMAIL = "bot@nongviet.vn"; // tai khoan AIBot do DataSeeder tao
    private static final int MAX_LENGTH = 1000;

    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();

    /** Lay cuoc tro chuyen cua khach; lan dau thi tao va chen loi chao cua tro ly AI. */
    public Conversation getOrCreateConversation(Integer customerId) {
        Conversation conv = conversationDAO.findByCustomerId(customerId);
        if (conv != null) return conv;

        User user = userDAO.findById(customerId);
        if (!(user instanceof Customer customer)) throw new BusinessException("Chỉ khách hàng mới dùng được chat.");
        Conversation fresh = new Conversation();
        fresh.setCustomer(customer);
        fresh.setCreatedAt(LocalDateTime.now());
        conversationDAO.save(fresh);

        User bot = userDAO.findByEmail(BOT_EMAIL);
        if (bot != null) {
            addMessage(fresh, bot, "Xin chào! Mình là trợ lý AI của NôngViệt. Bạn cần tư vấn gì về hạt giống, phân bón, "
                    + "thuốc BVTV hay máy móc nông nghiệp? Nhân viên shop sẽ hỗ trợ thêm khi cần.");
        }
        return fresh;
    }

    public List<Message> getMessagesAfter(Integer customerId, int afterId) {
        Conversation conv = getOrCreateConversation(customerId);
        return messageDAO.findAfter(conv.getId(), afterId);
    }

    public void sendFromCustomer(Integer customerId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException("Vui lòng nhập nội dung tin nhắn.");
        if (text.length() > MAX_LENGTH) throw new BusinessException("Tin nhắn tối đa " + MAX_LENGTH + " ký tự.");
        Conversation conv = getOrCreateConversation(customerId);
        addMessage(conv, userDAO.findById(customerId), text);
    }

    private void addMessage(Conversation conv, User sender, String content) {
        Message m = new Message();
        m.setConversation(conv);
        m.setSender(sender);
        m.setContent(content);
        m.setSentAt(LocalDateTime.now());
        messageDAO.save(m);
    }
}
