package com.perfume.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_history", indexes = {
    @Index(name = "idx_order_history_order", columnList = "order_id"),
    @Index(name = "idx_order_history_timestamp", columnList = "timestamp")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Order.OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String notes;

    @Column(length = 100)
    private String updatedBy; // Email of admin who updated, or "SYSTEM" for automatic updates

    public enum OrderStatus {
        PLACED,           // Order created after payment confirmation
        CONFIRMED,        // Admin confirmed the order
        PACKED,           // Order packed and ready
        SHIPPED,          // Order shipped
        DELIVERED,        // Order delivered to customer
        CANCELLED,        // Order cancelled
        REFUNDED          // Refund processed
    }
}