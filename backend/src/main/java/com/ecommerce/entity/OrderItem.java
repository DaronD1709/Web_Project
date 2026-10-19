package com.ecommerce.entity;

import jakarta.persistence.*;

/**
 * 1 dong hang trong don (bang "order_items"): san pham nao, so luong bao nhieu, gia luc dat.
 * Order va OrderItem la Composition (xoa don thi xoa dong hang). Khi huy don / duyet hoan hang, OrderDAO duyet qua cac OrderItem
 * de cong lai kho: Product.updateStock(+quantity). Lazy: JSP chi doc duoc khi DAO da JOIN FETCH (xem OrderDAO.findDetail).
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Aggregation: chi tham chieu Product.
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // So luong khach dat; khi huy / hoan hang chinh so nay duoc cong lai vao ton kho.
    private int quantity;

    // Snapshot gia luc dat hang - khong doi du Product doi gia sau nay.
    @Column(name = "price_at_order")
    private double priceAtOrder;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPriceAtOrder() { return priceAtOrder; }
    public void setPriceAtOrder(double priceAtOrder) { this.priceAtOrder = priceAtOrder; }
}
