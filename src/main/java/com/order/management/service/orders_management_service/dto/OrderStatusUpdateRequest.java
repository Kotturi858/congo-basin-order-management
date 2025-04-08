package com.order.management.service.orders_management_service.dto;

public class OrderStatusUpdateRequest {
    private String status;

    public OrderStatusUpdateRequest() {
    }

    public OrderStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
