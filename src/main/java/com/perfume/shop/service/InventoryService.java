package com.perfume.shop.service;

import com.perfume.shop.entity.Product;
import com.perfume.shop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;

    // Low stock threshold - products with stock below this number will trigger alerts
    private static final int LOW_STOCK_THRESHOLD = 5;

    /**
     * Get products with low stock (below threshold)
     */
    public List<Product> getLowStockProducts() {
        return productRepository.findByStockLessThanAndActiveTrue(LOW_STOCK_THRESHOLD);
    }

    /**
     * Get count of products with low stock
     */
    public long getLowStockCount() {
        return productRepository.countByStockLessThanAndActiveTrue(LOW_STOCK_THRESHOLD);
    }

    /**
     * Get products that are out of stock
     */
    public List<Product> getOutOfStockProducts() {
        return productRepository.findByStockEqualsAndActiveTrue(0);
    }

    /**
     * Get count of out of stock products
     */
    public long getOutOfStockCount() {
        return productRepository.countByStockEqualsAndActiveTrue(0);
    }

    /**
     * Check if product has sufficient stock for the requested quantity
     */
    public boolean hasSufficientStock(Long productId, int requestedQuantity) {
        Product product = productRepository.findById(productId).orElse(null);
        return product != null && product.getActive() && product.getStock() >= requestedQuantity;
    }

    /**
     * Get stock level for a product
     */
    public int getStockLevel(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        return product != null ? product.getStock() : 0;
    }

    /**
     * Check if any products in the order have low stock
     */
    public boolean hasLowStockItems(List<Long> productIds) {
        for (Long productId : productIds) {
            int stock = getStockLevel(productId);
            if (stock > 0 && stock < LOW_STOCK_THRESHOLD) {
                return true;
            }
        }
        return false;
    }
}