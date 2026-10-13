package com.ecommerce.service;

import com.ecommerce.dao.OrderDAO;
import com.ecommerce.dao.StatsDAO;
import com.ecommerce.dto.ChartData;
import com.ecommerce.dto.ChartPoint;
import com.ecommerce.dto.TopProduct;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Nghiep vu THONG KE cho Admin (trang Tong quan). Doanh thu tinh tren don dang xu ly + hoan tat, KHONG tinh don huy / hoan hang
 * (xem StatsDAO.COUNTED). Moi khoang thoi gian "N ngay qua" la N ngay tron ven ket thuc o HOM NAY (tinh ca hom nay).
 */
public class AdminStatsService {

    public static final int LOW_STOCK_LIMIT = 5;      // "sap het hang" = ton kho <= 5 (khop badge o trang san pham)
    public static final int LOW_STOCK_LIST_LIMIT = 15; // danh sach "Sap het hang" tren Tong quan lay toi ton kho 15

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");

    private final StatsDAO statsDAO = new StatsDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    // ------------------------------------------------------------------ doanh thu

    /** Doanh thu tung ngay trong `days` ngay gan nhat (ngay khong co don = 0), cu truoc moi sau. */
    public ChartData revenueByDay(int days) {
        LocalDate today = LocalDate.now();
        LocalDate first = today.minusDays(days - 1L);
        Map<LocalDate, Double> byDay = new TreeMap<>();
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) byDay.put(d, 0.0); // dien san 0 de ngay trong van co cot
        for (Object[] row : statsDAO.revenueRows(first.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            LocalDate day = ((LocalDateTime) row[0]).toLocalDate();
            byDay.merge(day, ((Number) row[1]).doubleValue(), Double::sum);
        }
        List<ChartPoint> points = new ArrayList<>();
        byDay.forEach((d, v) -> points.add(new ChartPoint(d.format(DAY_LABEL), v, null)));
        return new ChartData(points);
    }

    /** % thay doi so voi ky truoc (vd 6.4 = tang 6,4%); null neu ky truoc bang 0 (khong tinh duoc %). */
    public static Double deltaPercent(double current, double previous) {
        return previous == 0 ? null : (current - previous) / previous * 100;
    }

    /** Doanh thu hom nay va hom qua (de so sanh tren the Tong quan). */
    public double[] todayAndYesterday() {
        List<ChartPoint> pts = revenueByDay(2).getPoints();
        return new double[]{pts.get(1).getValue(), pts.get(0).getValue()};
    }

    public List<TopProduct> topProducts(int days, int limit) {
        LocalDate today = LocalDate.now();
        return statsDAO.topProducts(today.minusDays(days - 1L).atStartOfDay(), today.plusDays(1).atStartOfDay(), limit);
    }

    // ------------------------------------------------------------------ tong quan

    /** Don theo tung trang thai (nhieu nhat truoc, bo trang thai 0 don). */
    public ChartData ordersByStatus() {
        List<ChartPoint> points = new ArrayList<>();
        orderDAO.countByStatus().forEach((status, n) -> points.add(new ChartPoint(status.getLabel(), n, null)));
        points.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return new ChartData(points);
    }

    public long countStatus(OrderStatus status) {
        return orderDAO.countByStatus().getOrDefault(status, 0L);
    }

    public List<Product> lowStock() {
        return statsDAO.lowStock(LOW_STOCK_LIST_LIMIT, 8);
    }

    public long countLowStock() {
        return statsDAO.countLowStock(LOW_STOCK_LIMIT);
    }
}
