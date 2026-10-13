package com.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("VNPAY")
public class VNPayPayment extends Payment {

    @Column(name = "transaction_id")
    private String transactionId;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    @Override
    public String getMethod() {
        return "VNPay";
    }

    @Override
    public boolean processPayment() {
        // Chỉ khởi tạo trạng thái chờ thanh toán.
        // IPN đã xác thực mới được ghi nhận kết quả từ VNPay.
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }

        // Gọi lại không ghi đè kết quả SUCCESS/FAILED đã có.
        return this.status == PaymentStatus.PENDING;
    }
}
