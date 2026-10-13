package com.ecommerce.dto;

/** 1 dong "san pham ban chay": so luong da ban va doanh thu (khong tinh don huy/hoan hang) trong 1 khoang thoi gian. Chi de hien thi thong ke. */
public class TopProduct {

    private final Integer id;
    private final String name;
    private final String categoryName;
    private final long quantity;
    private final double revenue;

    public TopProduct(Integer id, String name, String categoryName, long quantity, double revenue) {
        this.id = id;
        this.name = name;
        this.categoryName = categoryName;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getCategoryName() { return categoryName; }
    public long getQuantity() { return quantity; }
    public double getRevenue() { return revenue; }
}
