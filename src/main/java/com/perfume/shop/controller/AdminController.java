package com.perfume.shop.controller;

import com.perfume.shop.dto.ApiResponse;
import com.perfume.shop.dto.OrderStatusUpdateRequest;
import com.perfume.shop.dto.ProductRequest;
import com.perfume.shop.dto.ProductResponse;
import com.perfume.shop.dto.UserResponse;
import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.OrderHistory;
import com.perfume.shop.entity.OrderItem;
import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.User;
import com.perfume.shop.repository.OrderHistoryRepository;
import com.perfume.shop.repository.OrderRepository;
import com.perfume.shop.repository.ProductRepository;
import com.perfume.shop.repository.UserRepository;
import com.perfume.shop.service.InventoryService;
import com.perfume.shop.service.OrderService;
import com.perfume.shop.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Controller - Administrative endpoints for managing products, orders, and users.
 * 
 * All endpoints require ADMIN or CUSTOMER role (should be ADMIN only in production).
 * Provides CRUD operations and statistics for the admin dashboard.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
@RequiredArgsConstructor
public class AdminController {
    
    private final ProductService productService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    
    /**
     * Create pageable with sort configuration.
     * 
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDir Sort direction (ASC or DESC)
     * @return Configured Pageable
     */
    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }
    
    // ==================== Product Management ====================
    
    /**
     * Get all products (including inactive) with pagination.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: createdAt)
     * @param sortDir Sort direction (default: DESC)
     * @return Page of all products
     */
    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponse>> getAllProductsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(productService.getAllProductsAdmin(pageable));
    }
    
    /**
     * Get products by active status.
     * 
     * @param active Filter by active status
     * @param page Page number
     * @param size Page size
     * @return Page of products filtered by status
     */
    @GetMapping("/products/status")
    public ResponseEntity<Page<ProductResponse>> getProductsByStatus(
            @RequestParam Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getProductsByStatus(active, pageable));
    }
    
    /**
     * Get single product by ID (including inactive).
     * 
     * @param id Product ID
     * @return Product details
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProductByIdAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductByIdAdmin(id));
    }
    
    /**
     * Create new product.
     * 
     * @param request Product creation request
     * @return Created product
     */
    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
    
    /**
     * Full update of existing product.
     * All fields are replaced with new values.
     * 
     * @param id Product ID
     * @param request Product update request
     * @return Updated product
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }
    
    /**
     * Partial update of existing product.
     * Only provided fields are updated.
     * 
     * @param id Product ID
     * @param request Partial product update request
     * @return Updated product
     */
    @PatchMapping("/products/{id}")
    public ResponseEntity<ProductResponse> partialUpdateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.partialUpdateProduct(id, request));
    }
    
    /**
     * Soft delete product (set active = false).
     * Product remains in database but is hidden from customers.
     * 
     * @param id Product ID
     * @return Success message
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated successfully"));
    }
    
    /**
     * Permanently delete product from database.
     * This action cannot be undone.
     * 
     * @param id Product ID
     * @return Success message
     */
    @DeleteMapping("/products/{id}/permanent")
    public ResponseEntity<ApiResponse> permanentDeleteProduct(@PathVariable Long id) {
        productService.permanentDeleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product permanently deleted"));
    }
    
    /**
     * Activate product (set active = true).
     * 
     * @param id Product ID
     * @return Updated product
     */
    @PatchMapping("/products/{id}/activate")
    public ResponseEntity<ProductResponse> activateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.activateProduct(id));
    }
    
    /**
     * Deactivate product (set active = false).
     * 
     * @param id Product ID
     * @return Updated product
     */
    @PatchMapping("/products/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivateProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.deactivateProduct(id));
    }
    
    /**
     * Toggle featured status.
     * 
     * @param id Product ID
     * @return Updated product
     */
    @PatchMapping("/products/{id}/featured")
    public ResponseEntity<ProductResponse> toggleFeatured(@PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleFeatured(id));
    }
    
    // ==================== Stock Management ====================
    
    /**
     * Update product stock (set absolute value).
     * 
     * @param id Product ID
     * @param quantity New stock quantity
     * @return Updated product
     */
    @PatchMapping("/products/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity
    ) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }
    
    /**
     * Adjust product stock (add or subtract).
     * Use positive values to add stock, negative to subtract.
     * 
     * @param id Product ID
     * @param adjustment Stock adjustment (can be negative)
     * @return Updated product
     */
    @PatchMapping("/products/{id}/stock/adjust")
    public ResponseEntity<ProductResponse> adjustStock(
            @PathVariable Long id,
            @RequestParam Integer adjustment
    ) {
        return ResponseEntity.ok(productService.adjustStock(id, adjustment));
    }
    
    /**
     * Get low stock products.
     * Returns products with stock below threshold.
     * 
     * @param threshold Stock threshold (default: 10)
     * @return List of low stock products
     */
    @GetMapping("/products/low-stock")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
            @RequestParam(defaultValue = "10") Integer threshold
    ) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }
    
    // ==================== Statistics ====================
    
    /**
     * Get product statistics
     */
    @GetMapping("/products/statistics")
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        Long totalActive = productService.getTotalActiveProducts();
        Long totalOutOfStock = productService.getTotalOutOfStockProducts();
        List<String> brands = productService.getAllBrands();
        List<String> categories = productService.getAllCategories();
        
        Map<String, Object> stats = Map.of(
                "totalActiveProducts", totalActive,
                "totalOutOfStockProducts", totalOutOfStock,
                "totalBrands", brands.size(),
                "totalCategories", categories.size(),
                "brands", brands,
                "categories", categories
        );
        
        return ResponseEntity.ok(stats);
    }
    
    // ==================== Order Management ====================
    
    /**
     * Get all orders with pagination.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: createdAt)
     * @param sortDir Sort direction (default: DESC)
     * @return Page of all orders
     */
    @GetMapping("/orders")
    public ResponseEntity<Page<Order>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }
    
    /**
     * Update order status with notes.
     * 
     * @param id Order ID
     * @param request Status update request with status and notes
     * @return Updated order
     */
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Order.OrderStatus status = Order.OrderStatus.valueOf(request.getStatus().toUpperCase());
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status, adminEmail, request.getNotes()));
    }
    
    /**
     * Update order status (legacy endpoint for backward compatibility).
     * 
     * @param id Order ID
     * @param status New order status
     * @return Updated order
     */
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateOrderStatusLegacy(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status
    ) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status, adminEmail, null));
    }
    
    /**
     * Update tracking number for order.
     * 
     * @param id Order ID
     * @param trackingNumber Shipping tracking number
     * @return Updated order
     */
    @PatchMapping("/orders/{id}/tracking")
    public ResponseEntity<Order> updateTrackingNumber(
            @PathVariable Long id,
            @RequestParam String trackingNumber
    ) {
        return ResponseEntity.ok(orderService.updateTrackingNumber(id, trackingNumber));
    }
    
    /**
     * Cancel an order (admin action).
     * 
     * @param id Order ID
     * @return Cancelled order
     */
    @PatchMapping("/orders/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
            order.getStatus() == Order.OrderStatus.DELIVERED ||
            order.getStatus() == Order.OrderStatus.PACKED) {
            throw new RuntimeException("Cannot cancel order in current status: " + order.getStatus());
        }
        
        // Restore stock for cancelled orders that have been paid
        if (order.getStatus() == Order.OrderStatus.PLACED || 
            order.getStatus() == Order.OrderStatus.CONFIRMED) {
            restoreStockForOrder(order);
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        
        // Create history entry
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        createOrderHistoryEntry(order, Order.OrderStatus.CANCELLED, adminEmail, "Order cancelled by admin");
        
        return ResponseEntity.ok(order);
    }
    
    // ==================== User Management ====================
    
    /**
     * Get all users with pagination.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: createdAt)
     * @param sortDir Sort direction (default: DESC)
     * @return Page of all users
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<User> users = userRepository.findAll(pageable);
        return ResponseEntity.ok(users.map(UserResponse::fromEntity));
    }
    
    /**
     * Get user by ID.
     * 
     * @param id User ID
     * @return User details
     * @throws RuntimeException if user not found
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
    
    /**
     * Block a user (set active = false).
     * Blocked users cannot log in.
     * 
     * @param id User ID
     * @return Success message
     * @throws RuntimeException if user not found
     */
    @PatchMapping("/users/{id}/block")
    public ResponseEntity<ApiResponse> blockUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully"));
    }
    
    /**
     * Unblock a user (set active = true).
     * 
     * @param id User ID
     * @return Success message
     * @throws RuntimeException if user not found
     */
    @PatchMapping("/users/{id}/unblock")
    public ResponseEntity<ApiResponse> unblockUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setActive(true);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully"));
    }
    
    /**
     * Toggle user active status (via request body - for frontend compatibility).
     * 
     * @param id User ID
     * @param body Request body with active field
     * @return Updated user
     * @throws RuntimeException if user not found
     */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        Boolean active = body.get("active");
        user.setActive(active != null ? active : false);
        userRepository.save(user);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
    
    /**
     * Update user role (via request body - for frontend compatibility).
     * 
     * @param id User ID
     * @param body Request body with role field
     * @return Updated user
     * @throws RuntimeException if user not found
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        String roleStr = body.get("role");
        user.setRole(User.Role.valueOf(roleStr));
        userRepository.save(user);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
    
    /**
     * Get dashboard statistics.
     * Returns counts of users, orders, and products.
     * 
     * @return Statistics map
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Long totalUsers = userRepository.count();
        Long totalOrders = orderService.countTotalOrders();
        Long totalProducts = productService.getTotalActiveProducts();
        
        // Inventory alerts
        long lowStockCount = inventoryService.getLowStockCount();
        long outOfStockCount = inventoryService.getOutOfStockCount();
        List<Product> lowStockProducts = inventoryService.getLowStockProducts();
        List<Product> outOfStockProducts = inventoryService.getOutOfStockProducts();
        
        Map<String, Object> stats = Map.of(
                "totalUsers", totalUsers,
                "totalOrders", totalOrders,
                "totalProducts", totalProducts,
                "lowStockCount", lowStockCount,
                "outOfStockCount", outOfStockCount,
                "lowStockProducts", lowStockProducts.stream()
                    .map(p -> Map.of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "stock", p.getStock(),
                        "threshold", 5
                    ))
                    .toList(),
                "outOfStockProducts", outOfStockProducts.stream()
                    .map(p -> Map.of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "stock", p.getStock()
                    ))
                    .toList(),
                "timestamp", System.currentTimeMillis()
        );
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Restore stock when order is cancelled
     */
    private void restoreStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int restoredStock = product.getStock() + item.getQuantity();
            product.setStock(restoredStock);
            productRepository.save(product);
        }
    }
    
    /**
     * Create order history entry
     */
    private void createOrderHistoryEntry(Order order, Order.OrderStatus status, String updatedBy, String notes) {
        OrderHistory history = OrderHistory.builder()
                .order(order)
                .status(status)
                .timestamp(java.time.LocalDateTime.now())
                .notes(notes)
                .updatedBy(updatedBy)
                .build();
        
        orderHistoryRepository.save(history);
    }
}
