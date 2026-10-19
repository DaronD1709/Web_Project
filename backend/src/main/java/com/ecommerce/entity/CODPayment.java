package com.ecommerce.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Thanh toan khi nhan hang (COD). Khong co buoc thanh toan online: tao ra o trang thai PENDING, den khi Admin danh dau don hoan tat
 * (OrderDAO.changeStatusInTransaction) moi doi sang SUCCESS va ghi ngay thu tien; neu don bi huy luc con PENDING thi sang FAILED.
 */
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
