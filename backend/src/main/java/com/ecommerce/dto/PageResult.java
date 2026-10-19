package com.ecommerce.dto;

import java.util.List;

/**
 * Ket qua 1 trang danh sach: cac phan tu cua trang + thong tin de ve thanh phan trang.
 * Dung chung cho danh sach don hang (Admin va khach), thong bao, san pham... T la kieu phan tu (vd Order). Khong phai entity, khong luu DB.
 * JSP doc: ${result.items} (cac dong cua trang), ${result.page} (trang hien tai), ${result.totalCount}, ${result.totalPages}.
 */
public class PageResult<T> {

    private final List<T> items;
    private final long totalCount;
    private final int page;
    private final int pageSize;

    public PageResult(List<T> items, long totalCount, int page, int pageSize) {
        this.items = items;
        this.totalCount = totalCount;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<T> getItems() { return items; }
    public long getTotalCount() { return totalCount; }
    public int getPage() { return page; }
    // So trang = lam tron LEN (tong so dong / so dong moi trang), vd 21 don, 10 don/trang -> 3 trang.
    public int getTotalPages() { return (int) Math.ceil((double) totalCount / pageSize); }
}
