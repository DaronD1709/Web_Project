package com.ecommerce.service;

import com.ecommerce.dao.VoucherDAO;
import com.ecommerce.entity.DiscountType;
import com.ecommerce.entity.Voucher;
import com.ecommerce.util.ParamUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Nghiep vu QUAN LY VOUCHER cua Admin: xem theo trang thai, tao, sua, bat/tat, xoa. Luat "voucher dung duoc khong" nam o Voucher.getState(). */
public class AdminVoucherService {

    private static final Pattern CODE = Pattern.compile("^[A-Za-z0-9]{3,20}$");

    private final VoucherDAO voucherDAO = new VoucherDAO();

    /** Danh sach theo tab: "all" hoac 1 trang thai ("active", "soldout", "expired", "off"). */
    public List<Voucher> list(String tab) {
        List<Voucher> all = voucherDAO.findAllNewestFirst();
        if ("all".equals(tab)) return all;
        return all.stream().filter(v -> v.getState().equals(tab)).toList();
    }

    /** So voucher moi tab (khoa "all", "active", ...) cho cac so tren tab. */
    public Map<String, Long> tabCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (Voucher v : voucherDAO.findAllNewestFirst()) {
            counts.merge(v.getState(), 1L, Long::sum);
            counts.merge("all", 1L, Long::sum);
        }
        return counts;
    }

    public Voucher getById(Integer id) {
        Voucher v = id == null ? null : voucherDAO.findById(id);
        if (v == null) throw new BusinessException("Voucher không tồn tại.");
        return v;
    }

    /**
     * Tao (id == null) hoac sua voucher. Nhan tham so dang CHUOI nguyen ban tu form de tu kiem tra va bao loi tung o
     * (BusinessException chua Map ten-o -> loi). Cac buoc: (1) kiem tra tung o (2) neu sua thi lay ban cu (3) gan gia tri (4) luu.
     */
    public Voucher save(Integer id, String code, String type, String value, String min, String issued,
                        String start, String end, boolean active) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Ma: chu/so 3-20 ky tu, luu IN HOA, khong trung voi voucher khac
        code = code == null ? "" : code.trim();
        if (!CODE.matcher(code).matches()) errors.put("code", "Mã chỉ gồm chữ và số, 3–20 ký tự");
        else {
            Voucher same = voucherDAO.findByCode(code);
            if (same != null && !same.getId().equals(id)) errors.put("code", "Mã này đã tồn tại");
        }

        // Loai giam + gia tri: phan tram 1-100, tien co dinh > 0
        DiscountType discountType = "fixed".equals(type) ? DiscountType.FIXED_AMOUNT : DiscountType.PERCENTAGE;
        double discountValue = parseNumber(value, -1);
        if (!(discountValue > 0)) errors.put("value", "Giá trị phải lớn hơn 0");
        else if (discountType == DiscountType.PERCENTAGE && discountValue > 100) errors.put("value", "Phần trăm giảm tối đa 100%");
        else if (discountValue > 1_000_000_000) errors.put("value", "Giá trị quá lớn");

        double minOrder = (min == null || min.isBlank()) ? 0 : parseNumber(min, -1);
        if (minOrder < 0) errors.put("min", "Đơn tối thiểu không hợp lệ");

        int quantity = (int) parseNumber(issued, -1);
        if (quantity < 1) errors.put("issued", "Số lượng phải ≥ 1");

        LocalDate startDay = ParamUtil.dateOrNull(start);
        LocalDate endDay = ParamUtil.dateOrNull(end);
        if (startDay == null) errors.put("start", "Vui lòng chọn ngày bắt đầu");
        if (endDay == null) errors.put("end", "Vui lòng chọn ngày hết hạn");
        else if (startDay != null && endDay.isBefore(startDay)) errors.put("end", "Ngày hết hạn phải sau ngày bắt đầu");

        Voucher voucher = new Voucher();
        if (id != null) {
            voucher = getById(id);
            // Sua so luong phat hanh: khong duoc nho hon so luot da dung
            if (quantity >= 1 && quantity < voucher.getQuantityUsed()) {
                errors.put("issued", "Không thể nhỏ hơn số lượt đã dùng (" + voucher.getQuantityUsed() + ")");
            }
        }
        if (!errors.isEmpty()) throw new BusinessException(errors);

        voucher.setCode(code.toUpperCase());
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(discountValue);
        voucher.setMinOrderValue(minOrder);
        voucher.setQuantityIssued(quantity);
        voucher.setStartDate(startDay.atStartOfDay());
        // het han vao CUOI ngay da chon. Khong dung LocalTime.MAX (23:59:59.999999999): Postgres chi luu den micro giay nen bi lam tron len 00:00 NGAY SAU
        voucher.setEndDate(endDay.atTime(23, 59, 59));
        voucher.setActive(active);
        return id == null ? voucherDAO.save(voucher) : voucherDAO.update(voucher);
    }

    /** Dat bat/tat theo trang thai MONG MUON (khong dao nguoc): bam nhanh 2 lan khong bi lech. Tra ve cau thong bao. */
    public String setActive(Integer id, boolean active) {
        Voucher v = getById(id);
        v.setActive(active);
        voucherDAO.update(v);
        return (active ? "Đã bật voucher " : "Đã tắt voucher ") + v.getCode();
    }

    /** Xoa voucher. Da co don hang dung thi tu choi (don hang can giu lai voucher da ap dung), thay vao do tat voucher. */
    public void delete(Integer id) {
        Voucher v = getById(id);
        long used = voucherDAO.countOrders(id);
        if (used > 0) {
            throw new BusinessException("Không thể xoá: voucher " + v.getCode() + " đã được dùng trong " + used
                    + " đơn hàng. Hãy tắt voucher thay vì xoá.");
        }
        voucherDAO.deleteById(id);
    }

    // So tu chuoi nguyen ban; sai dinh dang -> tra gia tri mac dinh `fallback` (de caller bao loi o do)
    private static double parseNumber(String s, double fallback) {
        try {
            return Double.parseDouble(s == null ? "" : s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
