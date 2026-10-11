package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "discount_value")
    private double discountValue;

    @Column(name = "min_order_value")
    private double minOrderValue;

    @Column(name = "quantity_issued")
    private int quantityIssued;

    @Column(name = "quantity_used")
    private int quantityUsed;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_active")
    private boolean isActive;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }
    public double getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }
    public int getQuantityIssued() { return quantityIssued; }
    public void setQuantityIssued(int quantityIssued) { this.quantityIssued = quantityIssued; }
    public int getQuantityUsed() { return quantityUsed; }
    public void setQuantityUsed(int quantityUsed) { this.quantityUsed = quantityUsed; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    /**
     * Trang thai voucher cho giao dien: "active" (dang dung duoc) | "expired" (chua toi han hoac het han) | "soldout" (het luot) | "off" (admin tat).
     * Day la NOI DUY NHAT dinh nghia 3 dieu kien hop le (bat + trong han + con luot); isValid() chi la "state == active".
     */
    public String getState() {
        if (!isActive) return "off";
        LocalDateTime now = LocalDateTime.now();
        if (startDate == null || endDate == null || now.isBefore(startDate) || now.isAfter(endDate)) return "expired";
        if (quantityUsed >= quantityIssued) return "soldout";
        return "active";
    }

    public boolean isValid() {
        return "active".equals(getState());
    }

    /** Ngay bat dau / het han dang "2026-10-31" cho o <input type="date"> (JSP khong doc truc tiep duoc LocalDateTime); rong neu chua co. */
    public String getStartDateText() {
        return startDate == null ? "" : startDate.toLocalDate().toString();
    }

    public String getEndDateText() {
        return endDate == null ? "" : endDate.toLocalDate().toString();
    }
}
