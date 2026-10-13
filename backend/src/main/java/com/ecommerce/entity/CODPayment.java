package com.ecommerce.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("COD")
public class CODPayment extends Payment {

    @Override
    public String getMethod() {
        return "COD";
    }

    @Override
    public boolean processPayment() {
        // COD: coi nhu PENDING cho toi khi giao hang thanh cong moi doi sang SUCCESS
        this.status = PaymentStatus.PENDING;
        return true;
    }
}
