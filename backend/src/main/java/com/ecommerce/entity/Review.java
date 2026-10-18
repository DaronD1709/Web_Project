package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
// Moi khach chi co 1 danh gia cho 1 san pham (gui lai = cap nhat). Rang buoc o DB giu dung ngay ca khi bam gui 2 lan cung luc.
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "customer_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Ngay danh gia dang "05/10/2026" cho JSP (JSTL khong doc duoc LocalDateTime). */
    @Transient
    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
