package com.order.management.service.orders_management_service.repository;

import com.order.management.service.orders_management_service.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    /**
     * Find all order items for a specific order
     * 
     * @param orderId the ID of the order
     * @return list of order items for the order
     */
    List<OrderItem> findByOrderId(Long orderId);
}
