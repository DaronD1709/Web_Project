package com.ecommerce.entity;

/** Trang thai don hang. label = ten hien thi cho nguoi dung (JSP doc bang ${order.status.label}); luat chuyen trang thai nam o Order.canMoveTo. */
public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    SHIPPING("Đang giao"),
    COMPLETED("Hoàn tất"),
    CANCELLED("Đã huỷ"),
    RETURN_REQUESTED("Yêu cầu hoàn hàng"),
    RETURNED("Đã hoàn hàng");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
