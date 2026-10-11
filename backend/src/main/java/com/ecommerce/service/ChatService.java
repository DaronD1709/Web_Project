package com.ecommerce.service;

import com.ecommerce.dao.MessageDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Message;
import com.ecommerce.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat giua khach va shop. Khong co bang "cuoc tro chuyen": moi Message gan truc tiep voi khach so huu luong chat
 * (Message.customer) va co nguoi gui (Message.sender = Customer hoac Admin).
 * Chatbot tra loi bang 1 tai khoan he thong loai Admin ("Tro ly AI", email BOT_EMAIL) do DataSeeder tao - khong co class AIBot.
 * Hien tai: khach gui va xem tin (polling bang htmx). Chatbot tu tra loi va phia Admin lam o nhanh feat/chat-with-shop.
 */
public class ChatService {

    public static final String BOT_EMAIL = "bot@nongviet.vn"; // tai khoan he thong cua chatbot (do DataSeeder tao)
    private static final int MAX_LENGTH = 1000;

    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ChatBotService chatBot = new ChatBotService();

    /** Tin nhan moi hon `afterId` cua khach. Lan dau khach mo chat (chua co tin nao) thi chen loi chao cua chatbot. */
    public List<Message> getMessagesAfter(Integer customerId, int afterId) {
        List<Message> messages = messageDAO.findAfter(customerId, afterId);
        if (afterId == 0 && messages.isEmpty()) {
            greet(customerId);
            messages = messageDAO.findAfter(customerId, afterId);
        }
        return messages;
    }

    public void sendFromCustomer(Integer customerId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException("Vui lòng nhập nội dung tin nhắn.");
        if (text.length() > MAX_LENGTH) throw new BusinessException("Tin nhắn tối đa " + MAX_LENGTH + " ký tự.");
        Customer customer = requireCustomer(customerId);
        addMessage(customer, customer, text);

        // Nhan vien dang tiep quan thi chatbot im lang; nguoc lai chatbot tra loi, neu khong hieu/khach xin gap nguoi thi chuyen cho nhan vien
        if (!customer.isHandledByHuman()) {
            User bot = userDAO.findByEmail(BOT_EMAIL);
            if (bot != null) {
                ChatBotService.Reply reply = chatBot.reply(text);
                addMessage(customer, bot, reply.text());
                if (reply.handoff()) setHandledByHuman(customerId, true);
            }
        }
    }

    /** Dat che do cuoc tro chuyen: true = nhan vien tiep quan (chatbot im), false = chatbot tra loi. Dung chung cho khach va Admin. */
    public void setHandledByHuman(Integer customerId, boolean human) {
        Customer customer = requireCustomer(customerId);
        if (customer.isHandledByHuman() == human) return; // khong doi thi khong ghi DB
        customer.setHandledByHuman(human);
        userDAO.update(customer);
    }

    private void greet(Integer customerId) {
        User bot = userDAO.findByEmail(BOT_EMAIL);
        if (bot == null) return; // chua seed tai khoan chatbot thi bo qua loi chao
        addMessage(requireCustomer(customerId), bot, "Xin chào! Mình là trợ lý AI của Nông Việt. Bạn cần tư vấn gì về hạt giống, "
                + "phân bón, thuốc BVTV hay máy móc nông nghiệp? Nhân viên shop sẽ hỗ trợ thêm khi cần.");
    }

    private Customer requireCustomer(Integer customerId) {
        User user = userDAO.findById(customerId);
        if (!(user instanceof Customer customer)) throw new BusinessException("Chỉ khách hàng mới dùng được chat.");
        return customer;
    }

    private void addMessage(Customer owner, User sender, String content) {
        Message m = new Message();
        m.setCustomer(owner);
        m.setSender(sender);
        m.setContent(content);
        m.setSentAt(LocalDateTime.now());
        messageDAO.save(m);
    }
}
