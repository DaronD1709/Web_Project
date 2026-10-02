package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// SINGLE_TABLE giong het ly do cua User: CODPayment/VNPayPayment gan nhu khong
// co cot rieng (chi VNPayPayment co transactionId), nen gop 1 bang la du.
@Entity
@Table(name = "payments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    protected Order order;

    protected double amount;

    @Column(name = "payment_date")
    protected LocalDateTime paymentDate;

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
}
