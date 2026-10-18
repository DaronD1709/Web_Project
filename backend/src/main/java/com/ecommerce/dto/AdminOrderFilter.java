package com.ecommerce.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * DTO gom tham so loc/phan trang cua trang Admin > Don hang (giong ProductFilter): status (tab), q (tu khoa), pay, from/to, page.
 * Khong phai entity, khong luu DB; chi di chuyen giua Servlet -> Service -> DAO.
 */
public class AdminOrderFilter {

    public static final int PAGE_SIZE = 10;

    private String status = "all";   // all | pending | confirmed | shipping | completed | cancelled | return (return = yeu cau hoan + da hoan)
    private String keyword;          // ma don, ten hoac email khach; null = khong tim
    private String pay = "";         // "" = moi loai | cod | vnpay
    private LocalDate from;          // tinh tu dau ngay nay (bao gom)
    private LocalDate to;            // den het ngay nay (bao gom)
    private int page = 1;

    public String getStatus() { return status; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getPay() { return pay; }
    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public int getPage() { return page; }
    public int getPageSize() { return PAGE_SIZE; }
    public int getOffset() { return (page - 1) * PAGE_SIZE; }

    // Chi nhan gia tri hop le -> DAO moi dung duoc status/pay de chon dieu kien an toan (khong noi chuoi nguoi dung vao JPQL).
    public void setStatus(String status) {
        boolean ok = "pending".equals(status) || "confirmed".equals(status) || "shipping".equals(status)
                || "completed".equals(status) || "cancelled".equals(status) || "return".equals(status);
        this.status = ok ? status : "all";
    }

    public void setPay(String pay) {
        this.pay = ("cod".equals(pay) || "vnpay".equals(pay)) ? pay : "";
    }

    public void setPage(int page) {
        this.page = Math.max(page, 1);
    }

    /** Query string cua bo loc hien tai (khong gom page) de JSP noi them &page=N; cung dung de nho "quay ve dung bo loc" sau khi doi trang thai don. */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder("status=" + status);
        if (keyword != null) sb.append("&q=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        if (!pay.isEmpty()) sb.append("&pay=").append(pay);
        if (from != null) sb.append("&from=").append(from);
        if (to != null) sb.append("&to=").append(to);
        return sb.toString();
    }
}
