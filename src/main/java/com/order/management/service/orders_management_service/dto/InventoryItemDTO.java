package com.order.management.service.orders_management_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
@Getter
public class InventoryItemDTO {
    private String productId;
    private String productName;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer lowStockThreshold;
    private Boolean isStockAvailable;
}
