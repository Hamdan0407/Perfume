package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimelineResponse {
    private Long id;
    private String status;
    private LocalDateTime timestamp;
    private String notes;
    private String updatedBy;
    private boolean isActive; // True if this is the current status
}