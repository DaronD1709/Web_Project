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
 * Chat giua khach va shop (nhan vien tra loi bang tay). Khong co bang "cuoc tro chuyen": moi Message gan truc tiep voi khach so huu luong chat
 * (Message.customer) va co nguoi gui (Message.sender = Customer hoac Admin).
 *
 * Phan cong trach nhiem:
 *   - Lop nay lo phia KHACH (gui tin, nhan tin, khach vang lai).
 *   - AdminChatService lo phia nhan vien; MessageDAO lo truy van.
 * Khong dung WebSocket: trinh duyet hoi lai server moi 3 giay (htmx polling), xem MessageDAO.findAfter.
 */
public class ChatService {

    private static final int MAX_LENGTH = 1000;      // do dai toi da 1 tin nhan, chan tin qua dai lam day DB
    private static final int MAX_GUEST_NAME = 60;    // do dai toi da ten khach vang lai

    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();

    /** Tin nhan moi hon `afterId` cua khach (afterId = 0 la lay tu dau, luc vua mo chat). Chi doc, khong tu them tin nao. */
    public List<Message> getMessagesAfter(Integer customerId, int afterId) {
        return messageDAO.findAfter(customerId, afterId);
    }

    /**
     * Khach gui 1 tin: (1) kiem tra noi dung  (2) luu tin. Nhan vien se thay tin o danh sach hoi thoai cua Admin (polling) va tra loi bang tay.
     */
    public void sendFromCustomer(Integer customerId, String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new BusinessException("Vui lòng nhập nội dung tin nhắn.");
        if (text.length() > MAX_LENGTH) throw new BusinessException("Tin nhắn tối đa " + MAX_LENGTH + " ký tự.");
        Customer customer = requireCustomer(customerId);
        addMessage(customer, customer, text);
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

    // Doc user theo id va bat buoc phai la Customer (Admin khong co luong chat nhu khach); sai thi bao loi nghiep vu.
    private Customer requireCustomer(Integer customerId) {
        User user = userDAO.findById(customerId);
        if (!(user instanceof Customer customer)) throw new BusinessException("Chỉ khách hàng mới dùng được chat.");
        return customer;
    }

    // Tao va luu 1 tin: owner = khach so huu luong chat (khoa de loc), sender = nguoi viet (khach hoac Admin), sentAt = bay gio.
    private void addMessage(Customer owner, User sender, String content) {
        Message m = new Message();
        m.setCustomer(owner);
        m.setSender(sender);
        m.setContent(content);
        m.setSentAt(LocalDateTime.now());
        messageDAO.save(m);
    }
}
