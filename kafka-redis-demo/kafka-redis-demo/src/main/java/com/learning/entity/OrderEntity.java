package com.learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
@Entity
@Builder
public class OrderEntity {
    @Id
    private String orderId;

    @Column(columnDefinition = "text", nullable = false)
    private String orderData;

    @Column(nullable = false)
    private Instant createdAt;
}
