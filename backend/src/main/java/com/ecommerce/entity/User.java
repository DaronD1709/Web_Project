package com.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Lop cha abstract. SINGLE_TABLE: Customer/Admin/AIBot deu luu chung 1 bang "users",
 * phan biet nhau qua cot discriminator "user_type". Don gian, nhanh, phu hop vi
 * cac lop con hau nhu khong co cot rieng (chi co quan he OneToMany/OneToOne).
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;

    @Column(nullable = false, unique = true)
    protected String email;

    @Column(name = "password_hash", nullable = false)
    protected String passwordHash;

    @Column(name = "full_name")
    protected String fullName;

    protected String phone;

    @Column(name = "created_at")
    protected LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
