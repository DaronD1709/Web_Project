package com.ecommerce.dto;

import java.util.List;

/** Du lieu de ve 1 bieu do: cac diem + gia tri lon nhat (JSP can max de tinh chieu cao/chieu dai thanh, EL khong tu tinh max cua danh sach duoc). */
public class ChartData {

    private final List<ChartPoint> points;
    private final double max;
    private final double total;

    public ChartData(List<ChartPoint> points) {
        this.points = points;
        this.max = points.stream().mapToDouble(ChartPoint::getValue).max().orElse(0);
        this.total = points.stream().mapToDouble(ChartPoint::getValue).sum();
    }

    public List<ChartPoint> getPoints() { return points; }
    public double getMax() { return max; }
    public double getTotal() { return total; }
}
