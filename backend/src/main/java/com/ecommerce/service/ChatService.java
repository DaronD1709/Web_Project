package com.ecommerce.service;

import com.ecommerce.dao.MessageDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Message;
import com.ecommerce.entity.User;
import com.ecommerce.util.PasswordUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chat giua khach va shop. Khong co bang "cuoc tro chuyen": moi Message gan truc tiep voi khach so huu luong chat
 * (Message.customer) va co nguoi gui (Message.sender = Customer hoac Admin).
 * Chatbot tra loi bang 1 tai khoan he thong loai Admin ("Tro ly tu dong", email BOT_EMAIL) do DataSeeder tao - khong co class AIBot.
 *
 * Phan cong trach nhiem:
 *   - Lop nay lo phia KHACH (gui tin, nhan tin, khach vang lai) va quy tac "bot hay nguoi dang tra loi" (handledByHuman).
 *   - ChatBotService chon cau tra loi tu cac luat Admin soan; AdminChatService lo phia nhan vien; MessageDAO lo truy van.
 * Khong dung WebSocket: trinh duyet hoi lai server moi 3 giay (htmx polling), xem MessageDAO.findAfter.
 */
public class ChatService {

    public static final String BOT_EMAIL = "bot@nongviet.vn"; // tai khoan he thong cua chatbot (do DataSeeder tao)
    private static final int MAX_LENGTH = 1000;      // do dai toi da 1 tin nhan, chan tin qua dai lam day DB
    private static final int MAX_GUEST_NAME = 60;    // do dai toi da ten khach vang lai

    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ChatBotService chatBot = new ChatBotService();

    /** Tin nhan moi hon `afterId` cua khach. Lan dau khach mo chat (chua co tin nao) thi chen loi chao cua chatbot. */
    public List<Message> getMessagesAfter(Integer customerId, int afterId) {
        // afterId == 0 nghia la trinh duyet chua co tin nao (vua mo chat); neu cuoc chat con trong thi chatbot chao truoc.
        List<Message> messages = messageDAO.findAfter(customerId, afterId);
        if (afterId == 0 && messages.isEmpty()) {
            greet(customerId);
            messages = messageDAO.findAfter(customerId, afterId);
        }
        return messages;
    }

    /**
     * Khach gui 1 tin. Cac buoc: (1) kiem tra noi dung  (2) luu tin cua khach  (3) neu KHONG co nhan vien tiep quan thi chatbot tra loi ngay.
     * Moi tin cua khach deu duoc luu truoc; chatbot loi (khong co tai khoan bot) thi chi bo qua phan tra loi, khong lam mat tin khach.
     */
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
                // reply = cau tra loi + co "handoff" (chuyen nhan vien). Luat Admin soan hoac khong hieu cau hoi deu co the dat handoff.
                ChatBotService.Reply reply = chatBot.reply(text);
                addMessage(customer, bot, reply.text());
                if (reply.handoff()) setHandledByHuman(customerId, true); // tu day bot im lang cho toi khi Admin bat lai che do AI
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

    /**
     * Khach CHUA dang nhap bat dau chat: tao 1 Customer "vang lai" chi de gan tin nhan (xem User.guest). Chi can nhap ten.
     * Tra ve khach vua tao; Servlet luu id vao session de nhan ra lan sau.
     */
    public Customer startGuest(String name) {
        String clean = name == null ? "" : name.replaceAll("\\p{Cntrl}", " ").trim().replaceAll("\\s+", " ");
        if (clean.length() < 2) throw new BusinessException("Vui lòng nhập tên của bạn (ít nhất 2 ký tự).");
        if (clean.length() > MAX_GUEST_NAME) throw new BusinessException("Tên tối đa " + MAX_GUEST_NAME + " ký tự.");
        Customer guest = new Customer();
        guest.setGuest(true);
        guest.setFullName(clean);
        guest.setEmail("guest-" + UUID.randomUUID() + "@guest.invalid"); // ten mien .invalid khong ton tai that
        guest.setPasswordHash(PasswordUtil.hash(UUID.randomUUID().toString())); // mat khau ngau nhien bi bo di -> khong ai dang nhap duoc
        guest.setCreatedAt(LocalDateTime.now());
        userDAO.save(guest); // khong tao Cart: khach vang lai khong mua hang
        return guest;
    }

    /** Khach vang lai theo id (luu trong session), hoac null neu id khong con hop le / khong phai khach vang lai. */
    public Customer findGuest(Integer id) {
        User user = id == null ? null : userDAO.findById(id);
        return user instanceof Customer c && c.isGuest() ? c : null;
    }

    // Loi chao dau tien cua chatbot (tin cua tai khoan he thong "Tro ly tu dong"); chi goi khi cuoc chat chua co tin nao.
    private void greet(Integer customerId) {
        User bot = userDAO.findByEmail(BOT_EMAIL);
        if (bot == null) return; // chua seed tai khoan chatbot thi bo qua loi chao
        addMessage(requireCustomer(customerId), bot, "Xin chào! Mình là trợ lý tự động của Nông Việt. Bạn cần tư vấn gì về hạt giống, "
                + "phân bón, thuốc BVTV hay máy móc nông nghiệp? Nhân viên shop sẽ hỗ trợ thêm khi cần.");
    }

    // Doc user theo id va bat buoc phai la Customer (Admin khong co luong chat nhu khach); sai thi bao loi nghiep vu.
    private Customer requireCustomer(Integer customerId) {
        User user = userDAO.findById(customerId);
        if (!(user instanceof Customer customer)) throw new BusinessException("Chỉ khách hàng mới dùng được chat.");
        return customer;
    }

    // Tao va luu 1 tin: owner = khach so huu luong chat (khoa de loc), sender = nguoi viet (khach/Admin/bot), sentAt = bay gio.
    private void addMessage(Customer owner, User sender, String content) {
        Message m = new Message();
        m.setCustomer(owner);
        m.setSender(sender);
        m.setContent(content);
        m.setSentAt(LocalDateTime.now());
        messageDAO.save(m);
    }
}
