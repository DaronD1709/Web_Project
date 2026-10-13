package com.ecommerce.service;

import com.ecommerce.dao.VoucherDAO;
import com.ecommerce.entity.DiscountType;
import com.ecommerce.entity.Voucher;

import java.util.Locale;

/** Quy tắc áp dụng voucher cho khách hàng. */
public class VoucherService {

    private final VoucherDAO voucherDAO = new VoucherDAO();

    public Voucher getByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("Vui lòng nhập mã giảm giá.");
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        Voucher voucher = voucherDAO.findByCode(normalizedCode);

        if (voucher == null) {
            throw new BusinessException("Mã giảm giá không tồn tại.");
        }

        return voucher;
    }

    public double calculateDiscount(Voucher voucher, double subtotal) {
        if (voucher == null) {
            throw new BusinessException("Mã giảm giá không tồn tại.");
        }

        if (!Double.isFinite(subtotal) || subtotal <= 0) {
            throw new BusinessException(
                    "Giỏ hàng chưa có sản phẩm hợp lệ.");
        }

        // Dùng quy tắc đã có trong entity, tránh định nghĩa lại
        // điều kiện thời hạn, bật/tắt và số lượt sử dụng.
        if (!voucher.isValid()) {
            switch (voucher.getState()) {
                case "expired" -> throw new BusinessException(
                        "Mã giảm giá chưa đến hạn hoặc đã hết hạn sử dụng.");
                case "soldout" -> throw new BusinessException(
                        "Mã giảm giá đã hết lượt sử dụng.");
                case "off" -> throw new BusinessException(
                        "Mã giảm giá đang tạm ngừng áp dụng.");
                default -> throw new BusinessException(
                        "Mã giảm giá không hợp lệ.");
            }
        }

        double minimum = voucher.getMinOrderValue();
        double value = voucher.getDiscountValue();
        DiscountType type = voucher.getDiscountType();

        if (!Double.isFinite(minimum) || minimum < 0
                || !Double.isFinite(value) || value <= 0
                || type == null
                || (type == DiscountType.PERCENTAGE && value > 100)) {
            throw new BusinessException(
                    "Cấu hình mã giảm giá không hợp lệ.");
        }

        if (subtotal < minimum) {
            String minimumText = String.format(
                    Locale.forLanguageTag("vi-VN"), "%,.0f₫", minimum);

            throw new BusinessException(
                    "Đơn hàng chưa đạt giá trị tối thiểu "
                    + minimumText + " để dùng mã này.");
        }

        double discount = type == DiscountType.PERCENTAGE
                ? subtotal * value / 100
                : value;

        // Làm tròn tiền giảm về đồng; không giảm quá tiền hàng.
        return Math.min(subtotal, Math.round(discount));
    }
}