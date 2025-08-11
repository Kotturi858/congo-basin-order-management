package com.order.management.service.orders_management_service.dto;

public record StockReservationRequest(Long productId, Integer quantity, String orderId) {
}
