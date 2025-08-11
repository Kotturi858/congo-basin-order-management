package com.order.management.service.orders_management_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.management.service.orders_management_service.dto.InventoryItemDTO;
import com.order.management.service.orders_management_service.dto.StockReservationRequest;
import com.order.management.service.orders_management_service.model.Order;
import com.order.management.service.orders_management_service.model.OrderItem;
import com.order.management.service.orders_management_service.repository.OrderRepository;
import com.order.management.service.orders_management_service.repository.OrderItemRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;


    @Autowired
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Order createOrder(Order order) {
        // Validate order
        if (order.getCustomerId() == null || order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have a customer ID and at least one item.");
        }

        // Save the order first
        Order savedOrder = orderRepository.save(order);
        List<OrderItem> items = new ArrayList<OrderItem>();

        // Set the order reference for each item and save them
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(savedOrder);
                OrderItem orderItem = orderItemRepository.save(item);
                // Reserve stock for the order
                Boolean isStockAvailable = reserveStock(orderItem);
                if (!isStockAvailable) {
                    // If stock is not available, mark the order as failed
                    savedOrder.setStatus("FAILED");
                    savedOrder.setUpdatedAt(LocalDateTime.now());
                    orderRepository.save(savedOrder);
                    throw new IllegalStateException("Insufficient stock for product ID: " + item.getProductId());
                }
            }
        }

        return savedOrder;
    }

    private Boolean reserveStock(OrderItem order) {
        try {
            HttpHeaders headers = new HttpHeaders();

            StockReservationRequest request = new StockReservationRequest(
                    order.getProductId(),
                    order.getQuantity(),
                    order.getId().toString()
            );
            HttpEntity<StockReservationRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "http://localhost:8082/inventory/reserve",
                    HttpMethod.POST,
                    entity,
                    String.class);

            String body = response.getBody();

            try {
                InventoryItemDTO dto = new ObjectMapper().readValue(body, InventoryItemDTO.class);
                return dto.getIsStockAvailable();
            } catch (Exception e) {
                // Not JSON DTO
                return false;
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // mark the order as failed if stock reservation fails
            System.err.println("Error reserving stock for order item: " + order.getId() + " - " + e.getMessage());
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Order updateOrderStatus(Long id, String status) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            return orderRepository.save(order);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
