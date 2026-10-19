package com.ecommerce.entity;

/**
 * Trang thai don hang. label = ten hien thi cho nguoi dung (JSP doc bang ${order.status.label}); luat chuyen trang thai nam o Order.canMoveTo.
 *
 * So do chuyen trang thai (mui ten = duoc phep chuyen):
 *   PENDING -> CONFIRMED -> SHIPPING -> COMPLETED -> RETURN_REQUESTED -> RETURNED
 *   PENDING / CONFIRMED -> CANCELLED                       (huy don: cong lai kho)
 *   RETURN_REQUESTED -> COMPLETED                          (Admin tu choi yeu cau hoan hang)
 * CANCELLED va RETURNED la trang thai CUOI, khong chuyen di dau nua.
 * Ten enum duoc luu thang vao cot "status" (EnumType.STRING) va dung lam gia tri o tab loc / form doi trang thai, nen KHONG doi ten tuy y.
 */
public enum OrderStatus {
    PENDING("Chờ xác nhận"),            // vua dat, cho Admin xac nhan
    CONFIRMED("Đã xác nhận"),           // Admin da xac nhan, chuan bi giao
    SHIPPING("Đang giao"),              // da giao cho don vi van chuyen
    COMPLETED("Hoàn tất"),              // giao thanh cong (COD: coi nhu da thu tien)
    CANCELLED("Đã huỷ"),                // bi huy (khach hoac cua hang), da hoan kho
    RETURN_REQUESTED("Yêu cầu hoàn hàng"), // khach xin hoan, cho Admin duyet hoac tu choi
    RETURNED("Đã hoàn hàng");           // Admin duyet hoan, da cong lai kho

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** Nhan tieng Viet hien tren giao dien (huy hieu trang thai, thong bao loi "Khong the chuyen tu ... sang ..."). */
    public String getLabel() {
        return label;
    }
}
