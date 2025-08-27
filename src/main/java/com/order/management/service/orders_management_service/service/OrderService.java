package com.order.management.service.orders_management_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.management.service.orders_management_service.Cache.ProductPriceService;
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
    private final ProductPriceService productPriceService;


    @Autowired
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, RestTemplate restTemplate, ProductPriceService productPriceService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
        this.productPriceService = productPriceService;
    }

    @Transactional
    public Order createOrder(Order order) {
        // Validate order
        if (order.getCustomerId() == null || order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have a customer ID and at least one item.");
        }

        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        double totalAmount = 0.0;
        for (OrderItem item : order.getItems()) {
            Double unitPrice = productPriceService.getProductPrice(item.getProductId());
            totalAmount += item.getQuantity() * unitPrice;
        }

        if (totalAmount != order.getTotalAmount()) {
            throw new IllegalArgumentException("Total amount does not match the sum of item prices.");
        }
        order.setTotalAmount(totalAmount);

        // Save the order first
        Order savedOrder = orderRepository.save(order);

        // Set the order reference for each item and save them
        for (OrderItem item : order.getItems()) {
            item.setOrder(savedOrder);
            OrderItem orderItem = orderItemRepository.save(item);
            // Reserve stock for the order
            Boolean isStockAvailable = reserveStock(orderItem);
            if (!isStockAvailable) {
                // If stock is not available, mark the order as failed
                item.setIsAvailable(false);
                orderItemRepository.save(item);
            }
        }
        double totalAmount1 = order.getItems().stream().filter(e -> e.getIsAvailable() == true).mapToDouble(OrderItem::getSubtotal).sum();

        // Update the order with the payment status and total amount
        order.setTotalAmount(totalAmount1);
        orderRepository.save(savedOrder);

        boolean isPaymentSuccess = makePayment(savedOrder);

        if (!isPaymentSuccess) {
            releaseStock(order.getItems());
//          savedOrder.setStatus("PAYMENT FAILED");
        }
        return savedOrder;
    }

    private void releaseStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            if (item.getIsAvailable()) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    StockReservationRequest request = new StockReservationRequest(item.getProductId(), item.getQuantity(), item.getId().toString());
                    HttpEntity<StockReservationRequest> entity = new HttpEntity<>(request, headers);
                    restTemplate.exchange("http://inventory-management-service/inventory/release", HttpMethod.POST, entity, String.class);
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    //implement a retry mechanism or log the error with exponential backoff
                    // else implement
                    //a scheduler to check whether the stock is released in the inventory service
                    System.err.println("Error releasing stock for order item: " + item.getId() + " - " + e.getMessage());
                }
            }
        }
    }

    private boolean makePayment(Order order) {
        System.out.println("Processing payment for order ID: " + order.getId() + ", Amount: " + order.getTotalAmount());
        Boolean isPaymentRecieved = restTemplate.getForObject("http://CONGO-BASIN-PAYMENT-SERVICE/api/fakePayment", Boolean.class);
        System.out.println("Payment status for order ID " + order.getId() + ": " + (Boolean.TRUE.equals(isPaymentRecieved) ? "Success" : "Failed"));

        if (Boolean.TRUE.equals(isPaymentRecieved)) {
            order.setStatus("PAID");
        } else {
            order.setStatus("PAYMENT FAILED");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return Boolean.TRUE.equals(isPaymentRecieved);
    }

    private Boolean reserveStock(OrderItem order) {
        try {
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
