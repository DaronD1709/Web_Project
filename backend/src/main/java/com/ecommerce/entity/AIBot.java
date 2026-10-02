package com.ecommerce.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

// Khong co field rieng. generateReply() la business logic (goi API AI ben ngoai),
// khong thuoc entity - se viet trong tang Service, khong phai o day.
@Entity
@DiscriminatorValue("AI_BOT")
public class AIBot extends User {
}
