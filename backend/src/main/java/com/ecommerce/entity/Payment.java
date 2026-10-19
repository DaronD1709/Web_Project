package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// SINGLE_TABLE giong het ly do cua User: CODPayment/VNPayPayment gan nhu khong
// co cot rieng (chi VNPayPayment co transactionId), nen gop 1 bang la du.
//
// Lien quan Quan ly don hang: Moi Order co 1 Payment. Trang thai thanh toan (PaymentStatus) KHONG do Admin tu tay sua, ma di theo don:
//   - COD: tao ra PENDING; khi Admin chuyen don sang COMPLETED thi OrderDAO doi sang SUCCESS (coi nhu da thu tien); huy don thi sang FAILED.
//   - VNPay: do IPN/QueryDr da xac thuc chu ky ghi nhan; Order.canMoveTo chan xac nhan/giao don VNPay chua thanh toan thanh cong (chi cho huy).
// Lop cha truu tuong; moi cach thanh toan tu dinh nghia processPayment() va getMethod() (Strategy dua tren ke thua).
@Entity
@Table(name = "payments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;

    // Don hang so huu thanh toan nay (1-1). Khoa ngoai order_id nam o bang payments.
    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    protected Order order;

    // So tien phai tra: bang Order.totalAmount tai thoi diem dat (da gom phi ship, da tru voucher).
    protected double amount;

    // Thoi diem thu tien thanh cong (COD: luc giao xong; VNPay: luc IPN bao thanh cong). null khi chua thanh toan.
    @Column(name = "payment_date")
    protected LocalDateTime paymentDate;

    // PENDING (chua thu) / SUCCESS (da thu) / FAILED (that bai hoac don bi huy); luu theo ten enum.
    @Enumerated(EnumType.STRING)
    protected PaymentStatus status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    // Moi loai thanh toan tu override - day la Strategy pattern dua tren ke thua.
    public abstract boolean processPayment();

    /** Ten phuong thuc de hien thi ("COD", "VNPay"); JSP doc bang ${order.payment.method}. */
    public abstract String getMethod();
}
