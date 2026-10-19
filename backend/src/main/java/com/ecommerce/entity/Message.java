package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Mot tin nhan chat (bang "messages"). KHONG co bang "cuoc tro chuyen" rieng: ca cuoc chat cua 1 khach la tap hop moi Message
 * co cung customer_id. Nho vay don gian (vua de giai thich khi van dap): mo chat = loc theo khach, tin moi nhat = id lon nhat.
 * Nguoi gui (sender) co the la: chinh khach, nhan vien Admin, hoac tai khoan he thong "Tro ly tu dong" (chatbot).
 * Dung chung cho phia khach (ChatServlet) va phia Admin (AdminChatServlet).
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Khach SO HUU luong chat nay: moi tin (cua khach lan cua shop/chatbot) deu gan voi 1 khach.
    // Khach chi thay tin co customer la minh; Admin loc theo khach de xem/tra loi.
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // User, khong phai Customer/Admin cu the - polymorphism: la Customer (khach nhan) hoac Admin (nhan vien shop,
    // hoac tai khoan he thong "Tro ly tu dong" cua chatbot - khong co class AIBot rieng).
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Noi dung tin; kieu TEXT (khong gioi han 255 ky tu nhu VARCHAR mac dinh). Service tu gioi han toi da 1000 ky tu.
    @Column(columnDefinition = "TEXT")
    private String content;

    // Thoi diem gui; dung sap xep danh sach hoi thoai (hoi thoai co tin moi nhat len dau). Sap xep trong 1 cuoc chat thi dung id.
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
