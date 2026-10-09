package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "total_amount")
    private double totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address shippingAddress;

    @ManyToOne
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

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

    public void confirmOrder() {
        if (status == OrderStatus.PENDING) status = OrderStatus.CONFIRMED;
    }

    public void cancelOrder() {
        if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED) {
            status = OrderStatus.CANCELLED;
            // TODO: goi Product.updateStock(+qty) cho tung OrderItem o tang Service
        }
    }

    /**
     * Luong trang thai hop le (CHI NOI DUY NHAT dinh nghia luat chuyen trang thai):
     * PENDING -> CONFIRMED -> SHIPPING -> COMPLETED; PENDING/CONFIRMED -> CANCELLED; COMPLETED -> RETURN_REQUESTED -> RETURNED
     * (Admin tu choi hoan hang thi RETURN_REQUESTED -> COMPLETED). CANCELLED va RETURNED la trang thai cuoi.
     */
    public boolean canMoveTo(OrderStatus next) {
        if (status == null || next == null) return false;
        return switch (status) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.COMPLETED;
            case COMPLETED -> next == OrderStatus.RETURN_REQUESTED;
            case RETURN_REQUESTED -> next == OrderStatus.RETURNED || next == OrderStatus.COMPLETED;
            default -> false; // CANCELLED, RETURNED
        };
    }

    /** Doi trang thai; chuyen sai luat thi nem loi (Service nen goi canMoveTo truoc de bao loi than thien). Hoan kho do Service lo. */
    public void updateStatus(OrderStatus next) {
        if (!canMoveTo(next)) throw new IllegalStateException("Khong the chuyen don tu " + status + " sang " + next);
        this.status = next;
    }

    public void requestReturn(String reason) {
        if (status == OrderStatus.COMPLETED) {
            status = OrderStatus.RETURN_REQUESTED;
            this.returnReason = reason;
        }
    }

    public double calculateTotal() {
        return items.stream().mapToDouble(i -> i.getPriceAtOrder() * i.getQuantity()).sum();
    }
}
