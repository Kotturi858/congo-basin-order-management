package com.order.management.service.orders_management_service.Cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.management.service.orders_management_service.dto.InventoryItemDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ProductPriceService {
    private final RestTemplate restTemplate;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    public ProductPriceService(RestTemplate restTemplate,
                               @Qualifier("productPricesCache") com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache) {
        this.restTemplate = restTemplate;
        this.caffeineCache = caffeineCache;
    }

    @Cacheable(value = "productPrices", key = "#productId")
    public Double getProductPrice(Long productId) {
        String url = inventoryServiceUrl + "/inventory/" + productId;
        ResponseEntity<String> productDetails = restTemplate.exchange(url, HttpMethod.GET,
                null, String.class);
        String body = productDetails.getBody();

        try {
            InventoryItemDTO dto = new ObjectMapper().readValue(body, InventoryItemDTO.class);
            return dto.getPrice();
        } catch (Exception e) {
            // Not JSON DTO
            System.out.println("Failed to parse response: " + e.getMessage()
                    + ". Response body: " + body);
            return 0.0;
        }
    }

    public Map<Object, Object> peekCache() {
        return caffeineCache.asMap();
    }
}
