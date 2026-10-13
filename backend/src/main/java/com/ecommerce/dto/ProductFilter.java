package com.ecommerce.dto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * DTO = "goi tham so": gom cac tham so loc/sap xep/phan trang cua trang /products
 * thanh 1 doi tuong de truyen Servlet -> Service -> DAO (thay vi 7 tham so roi).
 * Khong phai entity, khong luu DB - chi la du lieu di chuyen giua cac lop.
 */
public class ProductFilter {

    public static final int PAGE_SIZE = 9;           // trang khach hang
    public static final int LOW_STOCK_LIMIT = 5;     // "sap het" = ton kho <= 5 (khop badge "Chi con N" tren the san pham)

    private Integer categoryId;     // null = tat ca danh muc
    private String keyword;         // null = khong tim kiem
    private Double minPrice;
    private Double maxPrice;
    private boolean inStockOnly;
    private String stock = "";      // chi dung o trang Admin: "" = moi ton kho | in = con hang | low = sap het | out = het hang
    private String sort = "new";    // new | asc | desc | name | stock (2 gia tri cuoi chi Admin dung)
    private int page = 1;
    private int pageSize = PAGE_SIZE;

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
    public String getStock() { return stock; }
    public String getSort() { return sort; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    // Chi chap nhan gia tri hop le -> DAO co the dung stock/sort de chon dieu kien/ORDER BY an toan.
    public void setStock(String stock) {
        this.stock = ("in".equals(stock) || "low".equals(stock) || "out".equals(stock)) ? stock : "";
    }

    public void setSort(String sort) {
        boolean valid = "asc".equals(sort) || "desc".equals(sort) || "name".equals(sort) || "stock".equals(sort);
        this.sort = valid ? sort : "new";
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(pageSize, 1);
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
        if (!stock.isEmpty()) sb.append("&stock=").append(stock);
        sb.append("&sort=").append(sort);
        return sb.substring(1); // bo dau & dau tien
    }
}
