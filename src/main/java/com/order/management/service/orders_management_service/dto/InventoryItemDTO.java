package com.order.management.service.orders_management_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class InventoryItemDTO {
    private String productId;
    private String productName;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer lowStockThreshold;
    private boolean isStockAvailable;
    private Double price;
}
