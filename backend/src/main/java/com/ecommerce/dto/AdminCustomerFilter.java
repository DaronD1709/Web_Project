package com.ecommerce.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** DTO gom tham so loc/phan trang cua trang Admin > Khach hang (giong ProductFilter): q (tu khoa), status (trang thai tai khoan), page. */
public class AdminCustomerFilter {

    public static final int PAGE_SIZE = 10;

    private String keyword;          // ten, email hoac so dien thoai; null = khong tim
    private String status = "";      // "" = moi trang thai | active | locked
    private int page = 1;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getStatus() { return status; }
    public int getPage() { return page; }
    public int getPageSize() { return PAGE_SIZE; }
    public int getOffset() { return (page - 1) * PAGE_SIZE; }

    // Chi nhan gia tri hop le -> DAO moi dung status de chon dieu kien an toan
    public void setStatus(String status) {
        this.status = ("active".equals(status) || "locked".equals(status)) ? status : "";
    }

    public void setPage(int page) {
        this.page = Math.max(page, 1);
    }

    /** Query string cua bo loc hien tai (khong gom page) de JSP noi them &page=N va de nho "quay ve dung bo loc" sau khi khoa/mo khoa. */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder("status=" + status);
        if (keyword != null) sb.append("&q=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        return sb.toString();
    }
}
