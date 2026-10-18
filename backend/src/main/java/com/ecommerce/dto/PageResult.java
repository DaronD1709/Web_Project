package com.ecommerce.dto;

import java.util.List;

/** Ket qua 1 trang danh sach: cac phan tu cua trang + thong tin de ve thanh phan trang. */
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
    public int getTotalPages() { return (int) Math.ceil((double) totalCount / pageSize); }
}
