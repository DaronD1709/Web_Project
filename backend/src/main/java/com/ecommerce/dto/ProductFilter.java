package com.ecommerce.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * DTO = "goi tham so": gom cac tham so loc/sap xep/phan trang cua trang /products
 * thanh 1 doi tuong de truyen Servlet -> Service -> DAO (thay vi 7 tham so roi).
 * Khong phai entity, khong luu DB - chi la du lieu di chuyen giua cac lop.
 */
public class ProductFilter {

    public static final int PAGE_SIZE = 9;

    private Integer categoryId;     // null = tat ca danh muc
    private String keyword;         // null = khong tim kiem
    private Double minPrice;
    private Double maxPrice;
    private boolean inStockOnly;
    private String sort = "new";    // new | asc | desc
    private int page = 1;

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    public boolean isInStockOnly() { return inStockOnly; }
    public void setInStockOnly(boolean inStockOnly) { this.inStockOnly = inStockOnly; }
    public String getSort() { return sort; }
    public int getPage() { return page; }
    public int getPageSize() { return PAGE_SIZE; }
    public int getOffset() { return (page - 1) * PAGE_SIZE; }

    // Chi chap nhan gia tri hop le -> DAO co the dung sort de chon ORDER BY an toan.
    public void setSort(String sort) {
        this.sort = ("asc".equals(sort) || "desc".equals(sort)) ? sort : "new";
    }

    public void setPage(int page) {
        this.page = Math.max(page, 1);
    }

    /** Query string cua bo loc hien tai (khong gom "page") de JSP noi them &page=N khi tao link phan trang. */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();
        if (categoryId != null) sb.append("&cat=").append(categoryId);
        if (keyword != null) sb.append("&q=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
        if (minPrice != null) sb.append("&min=").append(minPrice.longValue());
        if (maxPrice != null) sb.append("&max=").append(maxPrice.longValue());
        if (inStockOnly) sb.append("&instock=on");
        sb.append("&sort=").append(sort);
        return sb.substring(1); // bo dau & dau tien
    }
}
