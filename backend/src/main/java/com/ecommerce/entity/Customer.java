package com.ecommerce.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CUSTOMER")
public class Customer extends User {

    // cascade=ALL + orphanRemoval: xoa Customer thi xoa theo Address (composition, da thiet ke)
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Cart cart;

    // Chat voi shop: true = nhan vien (Admin) dang tiep quan cuoc tro chuyen, chatbot KHONG tu tra loi nua; false = chatbot tra loi.
    // Boolean + default false de cac dong users CO SAN (cot moi them bang hbm2ddl=update) khong bi null; isHandledByHuman() coi null la false.
    @Column(name = "handled_by_human", columnDefinition = "boolean default false")
    private Boolean handledByHuman = false;

    public boolean isHandledByHuman() { return handledByHuman != null && handledByHuman; }
    public void setHandledByHuman(boolean handledByHuman) { this.handledByHuman = handledByHuman; }

    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }
    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
}
