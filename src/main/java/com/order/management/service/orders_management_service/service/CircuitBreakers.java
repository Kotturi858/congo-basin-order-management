package com.order.management.service.orders_management_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.management.service.orders_management_service.dto.InventoryItemDTO;
import com.order.management.service.orders_management_service.dto.StockReservationRequest;
import com.order.management.service.orders_management_service.model.OrderItem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CircuitBreakers {

    private final RestTemplate restTemplate;

    public CircuitBreakers(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "reserveStockFallback")
    public Boolean reserveStock(OrderItem order) {
        System.out.println("Attempting to reserve stock for order item: " + order.getId());
        HttpHeaders headers = new HttpHeaders();

        StockReservationRequest request = new StockReservationRequest(order.getProductId(), order.getQuantity(), order.getId().toString());
        HttpEntity<StockReservationRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange("http://inventory-management-service/inventory/reserve", HttpMethod.POST, entity, String.class);

        String body = response.getBody();

        try {
            InventoryItemDTO dto = new ObjectMapper().readValue(body, InventoryItemDTO.class);
            return dto.isStockAvailable();
        } catch (Exception e) {
            // Not JSON DTO
            System.out.println("Failed to parse response: " + e.getMessage() + ". Response body: " + body);
            return false;
        }
    }

    private Boolean reserveStockFallback(OrderItem order, Throwable t) {
        System.err.println("Inventory service is down. Fallback method invoked for order item: " + order.getId() + " - " + t.getMessage());
        return false;
    }
}
