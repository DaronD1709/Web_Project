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
    public boolean processPayment() {
        // TODO: goi API VNPay that o day, luu transactionId tra ve
        this.status = PaymentStatus.SUCCESS;
        return true;
    }
}
