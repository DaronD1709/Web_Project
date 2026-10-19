package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Don hang (bang "orders"). Day la thuc the TRUNG TAM cua chuc nang Quan ly don hang:
 *   - Admin xem/loc/doi trang thai don (AdminOrderServlet -> AdminOrderService -> OrderDAO);
 *   - Khach dat don, theo doi, huy hoac xin hoan hang (OrderService).
 * Don KHONG tu nhan tien/nhan hang; no chi giu trang thai (OrderStatus) va cac moi quan he den khach, dong hang, thanh toan, dia chi, voucher.
 * LUAT chuyen trang thai nam DUY NHAT o canMoveTo()/updateStatus() ben duoi, de Admin va Khach khong the doi tuy y.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Khach da dat don. @ManyToOne mac dinh EAGER nen JSP doc duoc ${order.customer.fullName} ma khong can JOIN FETCH rieng.
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Thoi diem dat don (dung de sap xep danh sach, loc theo ngay va ve bieu do doanh thu).
    @Column(name = "order_date")
    private LocalDateTime orderDate;

    // Luu theo TEN enum (EnumType.STRING) chu khong theo so thu tu, de them/sap xep lai gia tri enum sau nay khong lam sai du lieu cu.
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // Tong tien khach phai tra (da gom phi ship, da tru voucher); duoc ghi 1 lan luc dat don, khong tinh lai khi xem.
    @Column(name = "total_amount")
    private double totalAmount;

    // Cac dong hang. Composition: xoa don thi xoa luon dong hang (cascade ALL + orphanRemoval).
    // LAZY: JSP chi doc duoc khi DAO da JOIN FETCH (OrderDAO.findDetail), neu khong bi LazyInitializationException.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Thanh toan cua don: CODPayment hoac VNPayPayment (chung bang "payments", phan biet bang cot discriminator).
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    // Don chi GIU THAM CHIEU toi Address (khong sao chep), nen dia chi da dung trong don bi khoa khong cho sua.
    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address shippingAddress;

    // Voucher da ap (co the null). Vi la khoa ngoai nen khong xoa duoc voucher da co don dung.
    @ManyToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    // Ly do khach xin hoan hang (chi co gia tri khi status la RETURN_REQUESTED hoac RETURNED).
    @Column(name = "return_reason")
    private String returnReason;

    // Snapshot luc dat hang (voucher/phi ship co the doi sau nay nen khong tinh nguoc duoc): totalAmount = tong hang - discountAmount + shippingFee.
    @Column(name = "discount_amount")
    private double discountAmount;

    @Column(name = "shipping_fee")
    private double shippingFee;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public Address getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Address shippingAddress) { this.shippingAddress = shippingAddress; }
    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }
    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    /** Ngay gio dat dang "05/10/2026 14:30" cho JSP (JSTL fmt:formatDate khong doc duoc LocalDateTime), vd ${order.orderDateText}. */
    public String getOrderDateText() {
        return orderDate == null ? "" : orderDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }
    public double getShippingFee() { return shippingFee; }
    public void setShippingFee(double shippingFee) { this.shippingFee = shippingFee; }

    /** Admin xac nhan don: PENDING -> CONFIRMED. Sai luat thi khong lam gi (khong nem loi). */
    public void confirmOrder() {
        if (canMoveTo(OrderStatus.CONFIRMED)) updateStatus(OrderStatus.CONFIRMED);
    }

    /**
     * Huy don: PENDING/CONFIRMED -> CANCELLED. Phuong thuc nay CHI doi trang thai; viec cong lai ton kho cho tung OrderItem
     * do tang DAO/Service lam trong cung transaction (OrderDAO.applyStatusChange), vi entity khong duoc phep tu ghi DB.
     */
    public void cancelOrder() {
        if (canMoveTo(OrderStatus.CANCELLED)) {
            updateStatus(OrderStatus.CANCELLED);
        }
    }

    /**
     * Luong trang thai hop le (CHI NOI DUY NHAT dinh nghia luat chuyen trang thai):
     * PENDING -> CONFIRMED -> SHIPPING -> COMPLETED; PENDING/CONFIRMED -> CANCELLED; COMPLETED -> RETURN_REQUESTED -> RETURNED
     * (Admin tu choi hoan hang thi RETURN_REQUESTED -> COMPLETED). CANCELLED va RETURNED la trang thai cuoi.
     */
    public boolean canMoveTo(OrderStatus next) {
        if (status == null || next == null) {
            return false;
        }

        // VNPay phải thanh toán thành công trước khi xử lý giao hàng.
        // Vẫn cho huỷ nếu trạng thái đơn cho phép.
        if (payment instanceof VNPayPayment
                && payment.getStatus() != PaymentStatus.SUCCESS
                && next != OrderStatus.CANCELLED) {
            return false;
        }

        // Giữ luật chuyển trạng thái tại một nơi dùng chung.
        return switch (status) {
            case PENDING ->
                next == OrderStatus.CONFIRMED
                || next == OrderStatus.CANCELLED;

            case CONFIRMED ->
                next == OrderStatus.SHIPPING
                || next == OrderStatus.CANCELLED;

            case SHIPPING -> next == OrderStatus.COMPLETED;

            case COMPLETED -> next == OrderStatus.RETURN_REQUESTED;

            case RETURN_REQUESTED ->
                next == OrderStatus.RETURNED
                || next == OrderStatus.COMPLETED;

            default -> false;
        };
    }

    /** Doi trang thai; chuyen sai luat thi nem loi (Service nen goi canMoveTo truoc de bao loi than thien). Hoan kho do Service lo. */
    public void updateStatus(OrderStatus next) {
        if (!canMoveTo(next)) throw new IllegalStateException("Khong the chuyen don tu " + status + " sang " + next);
        this.status = next;
    }

    /** Khach xin hoan hang: COMPLETED -> RETURN_REQUESTED, kem ly do. Admin se duyet (RETURNED) hoac tu choi (quay lai COMPLETED). */
    public void requestReturn(String reason) {
        if (canMoveTo(OrderStatus.RETURN_REQUESTED)) {
            updateStatus(OrderStatus.RETURN_REQUESTED);
            this.returnReason = reason;
        }
    }

    /** Tong tien HANG (gia luc dat x so luong), CHUA tru voucher va CHUA cong phi ship; dung hien "Tam tinh" o trang chi tiet don. */
    public double calculateTotal() {
        return items.stream().mapToDouble(i -> i.getPriceAtOrder() * i.getQuantity()).sum();
    }
}
