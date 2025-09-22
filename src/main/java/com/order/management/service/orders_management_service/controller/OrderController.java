package com.order.management.service.orders_management_service.controller;

import com.order.management.service.orders_management_service.dto.OrderStatusUpdateRequest;
import com.order.management.service.orders_management_service.model.Order;
import com.order.management.service.orders_management_service.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order
     * 
     * @param order The order to create
     * @return The created order with generated ID
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order createdOrder = orderService.createOrder(order);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    /**
     * Get order by ID
     * 
     * @param id The ID of the order to retrieve
     * @return The order if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        if (order != null) {
            return new ResponseEntity<>(order, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get all orders for a customer
     * 
     * @param id The customer ID
     * @return List of orders for the customer
     */
    @GetMapping("/customers/{id}")
    public ResponseEntity<List<Order>> getOrdersByCustomerId(@PathVariable Long id) {
        List<Order> customerOrders = orderService.getOrdersByCustomerId(id);
        return new ResponseEntity<>(customerOrders, HttpStatus.OK);
    }

    /**
     * Update order status
     * 
     * @param id The ID of the order to update
     * @param request The status update request
     * @return The updated order
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody OrderStatusUpdateRequest request) {
        
        Order updatedOrder = orderService.updateOrderStatus(id, request.getStatus());
        if (updatedOrder != null) {
            return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
