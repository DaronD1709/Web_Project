package com.ecommerce.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

// Khong co field rieng - dung het field ke thua tu User.
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {
}
