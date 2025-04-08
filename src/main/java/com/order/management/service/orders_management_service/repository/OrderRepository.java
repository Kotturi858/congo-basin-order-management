package com.order.management.service.orders_management_service.repository;

import com.order.management.service.orders_management_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find all orders for a specific customer
     * 
     * @param customerId the ID of the customer
     * @return list of orders for the customer
     */
    List<Order> findByCustomerId(Long customerId);
}
