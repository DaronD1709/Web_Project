package com.ecommerce.dto;

/** 1 diem/1 thanh cua bieu do: nhan (ngay, ten danh muc, ten san pham...), gia tri, va dong phu (vd ten danh muc cua san pham). Chi de ve bieu do, khong phai entity. */
public class ChartPoint {

    private final String label;
    private final double value;
    private final String sub;

    public ChartPoint(String label, double value, String sub) {
        this.label = label;
        this.value = value;
        this.sub = sub;
    }

    public String getLabel() { return label; }
    public double getValue() { return value; }
    public String getSub() { return sub; }
}
