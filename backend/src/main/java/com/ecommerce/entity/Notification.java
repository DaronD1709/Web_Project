package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thong bao gui cho nguoi dung (bang "notifications"). Trong chuc nang Quan ly don hang, MOI LAN Admin doi trang thai don,
 * OrderDAO.applyStatusChange tao 1 Notification cho khach cua don do (trong cung transaction doi trang thai).
 * Khach xem o trang Thong bao (huy hieu chuong cap nhat moi 30 giay bang htmx); isRead = da doc hay chua.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // User, khong phai Customer - vi Admin cung co the nhan notification sau nay.
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Noi dung hien cho khach, vd "Don hang #12 dang duoc giao den ban." (do AdminOrderService.changeStatus dung).
    private String message;

    // Cot is_read (khong dat ten "read" vi trung tu khoa SQL). false = chua doc -> tinh vao huy hieu chua doc.
    @Column(name = "is_read")
    private boolean isRead;

    // Thoi diem tao; thong bao moi nhat hien tren cung.
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Ngay gio dang "05/10/2026 14:30" cho JSP (JSTL khong doc duoc LocalDateTime). @Transient: la getter tinh, khong phai cot DB. */
    @Transient
    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
