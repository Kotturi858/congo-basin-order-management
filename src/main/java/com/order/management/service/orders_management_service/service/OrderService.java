package com.order.management.service.orders_management_service.service;

import com.order.management.service.orders_management_service.model.Order;
import com.order.management.service.orders_management_service.model.OrderItem;
import com.order.management.service.orders_management_service.repository.OrderRepository;
import com.order.management.service.orders_management_service.repository.OrderItemRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public Order createOrder(Order order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        if (order.getStatus() == null) {
            order.setStatus("PENDING");
        }
        
        // Save the order first
        Order savedOrder = orderRepository.save(order);
        
        // Set the order reference for each item and save them
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(savedOrder);
                orderItemRepository.save(item);
            }
        }
        
        return savedOrder;
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
