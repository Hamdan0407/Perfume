package com.perfume.shop.service;

import com.perfume.shop.dto.*;
import com.perfume.shop.entity.*;
import com.perfume.shop.repository.CartRepository;
import com.perfume.shop.repository.OrderHistoryRepository;
import com.perfume.shop.repository.OrderRepository;
import com.perfume.shop.repository.ProductRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
        /**
         * Generate PDF invoice for an order using OpenPDF
         */
        public byte[] generateInvoicePdf(Order order) {
            try {
                com.lowagie.text.Document document = new com.lowagie.text.Document();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                com.lowagie.text.pdf.PdfWriter.getInstance(document, baos);
                document.open();

                document.add(new com.lowagie.text.Paragraph("Invoice for Order: " + order.getOrderNumber()));
                document.add(new com.lowagie.text.Paragraph("Date: " + order.getCreatedAt()));
                document.add(new com.lowagie.text.Paragraph("Customer: " + order.getUser().getEmail()));
                document.add(new com.lowagie.text.Paragraph("Shipping Address: " + order.getShippingAddress() + ", " + order.getShippingCity() + ", " + order.getShippingCountry() + " - " + order.getShippingZipCode()));
                document.add(new com.lowagie.text.Paragraph("Phone: " + order.getShippingPhone()));
                document.add(new com.lowagie.text.Paragraph("Payment Method: " + order.getPaymentMethod()));

                document.add(new com.lowagie.text.Paragraph("\nOrder Items:"));
                com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
                table.addCell("Product");
                table.addCell("Qty");
                table.addCell("Price");
                table.addCell("Subtotal");
                for (OrderItem item : order.getItems()) {
                    table.addCell(item.getProduct().getName());
                    table.addCell(String.valueOf(item.getQuantity()));
                    table.addCell(item.getPrice().toString());
                    table.addCell(item.getSubtotal().toString());
                }
                document.add(table);

                document.add(new com.lowagie.text.Paragraph("\nSubtotal: " + order.getSubtotal()));
                document.add(new com.lowagie.text.Paragraph("Tax: " + order.getTax()));
                document.add(new com.lowagie.text.Paragraph("Shipping: " + order.getShippingCost()));
                document.add(new com.lowagie.text.Paragraph("Total: " + order.getTotalAmount()));

                document.add(new com.lowagie.text.Paragraph("\nThank you for your order!"));
                document.close();
                return baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate invoice PDF", e);
            }
        }
    
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final EmailService emailService;
    private final RazorpayService razorpayService;
    
    @Value("${app.stripe.api-key}")
    private String stripeApiKey;
    
    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;
    
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("10.00");
    
    /**
     * Transactional checkout flow with Razorpay integration.
     * Stock validation and price locking are done, but stock is NOT deducted until payment confirmation.
     * Uses pessimistic locking to prevent race conditions.
     * Creates a Razorpay order for payment processing.
     */
    @Transactional
    public RazorpayOrderResponse createOrder(User user, CheckoutRequest request) {
        log.info("Creating order for user: {}", user.getId());
        
        // Step 1: Retrieve and validate cart
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        // Step 2: Lock products with pessimistic write lock to prevent concurrent modifications
        List<Long> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toList());
        
        List<Product> lockedProducts = productRepository.findAllByIdWithLock(productIds);
        var productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        
        // Step 3: Validate stock availability with locked products
        StringBuilder stockErrors = new StringBuilder();
        for (CartItem item : cart.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            
            if (product == null) {
                stockErrors.append("Product not found: ").append(item.getProduct().getName()).append(". ");
                continue;
            }
            
            if (!product.getActive()) {
                stockErrors.append("Product no longer available: ").append(product.getName()).append(". ");
                continue;
            }
            
            if (product.getStock() < item.getQuantity()) {
                stockErrors.append("Insufficient stock for ").append(product.getName())
                        .append(". Available: ").append(product.getStock())
                        .append(", Requested: ").append(item.getQuantity()).append(". ");
            }
        }
        
        if (stockErrors.length() > 0) {
            throw new RuntimeException("Stock validation failed: " + stockErrors.toString());
        }
        
        // Step 4: Lock prices - capture current prices from locked products
        for (CartItem item : cart.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            BigDecimal currentPrice = product.getDiscountPrice() != null 
                    ? product.getDiscountPrice() 
                    : product.getPrice();
            item.setPrice(currentPrice);
        }
        
        // Step 5: Calculate totals with locked prices
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal tax = subtotal.multiply(TAX_RATE);
        BigDecimal total = subtotal.add(tax).add(SHIPPING_COST);
        
        // Step 6: Generate unique order number
        String orderNumber = generateOrderNumber();
        
        // Step 7: Create order with locked prices and validated stock
        Order order = Order.builder()
                .user(user)
                .orderNumber(orderNumber)
                .subtotal(subtotal)
                .tax(tax)
                .shippingCost(SHIPPING_COST)
                .totalAmount(total)
                .status(Order.OrderStatus.PLACED)
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingCountry(request.getShippingCountry())
                .shippingZipCode(request.getShippingZipCode())
                .shippingPhone(request.getShippingPhone())
                .paymentMethod("razorpay")
                .build();
        
        // Step 8: Create order items with locked prices
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            Product product = productMap.get(cartItem.getProduct().getId());
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .build();
            orderItems.add(orderItem);
        }
        
        order.setItems(orderItems);
        
        // Step 9: Persist order (stock not deducted yet)
        order = orderRepository.save(order);
        log.info("Order created: {} with total: {}", orderNumber, total);
        
        // Step 10: Create Razorpay Order
        // Convert amount to paise (multiply by 100 for INR)
        Long amountInPaise = total.multiply(new BigDecimal("100")).longValue();
        
        RazorpayOrderRequest razorpayRequest = RazorpayOrderRequest.builder()
                .amount(amountInPaise)
                .currency("INR")
                .receipt(orderNumber)
                .customerId(user.getId().toString())
                .customerName(user.getEmail())
                .customerEmail(user.getEmail())
                .customerPhone(request.getShippingPhone())
                .build();
        
        RazorpayOrderResponse razorpayResponse;
        try {
            razorpayResponse = razorpayService.createRazorpayOrder(razorpayRequest);
            
            // Step 11: Save Razorpay Order ID to our order
            order.setRazorpayOrderId(razorpayResponse.getRazorpayOrderId());
            order = orderRepository.save(order);
            
            // Step 12: Return response with Razorpay details for frontend
            return razorpayResponse.toBuilder()
                    .orderId(order.getId())
                    .orderNumber(orderNumber)
                    .build();
            
        } catch (Exception e) {
            log.error("Razorpay order creation failed for order: {}", orderNumber, e);
            throw new RuntimeException("Payment initialization failed: " + e.getMessage());
        }
    }
    
    /**
     * Confirm payment after successful Razorpay payment.
     * Called by webhook or frontend after payment verification.
     * Atomically deducts stock and updates order status.
     */
    @Transactional
    public Order confirmPayment(String razorpayOrderId, String razorpayPaymentId) {
        log.info("Confirming payment for Razorpay order: {}", razorpayOrderId);
        
        // Step 1: Find order by Razorpay order ID
        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Order not found for Razorpay order ID: " + razorpayOrderId));
        
        // Step 2: Check idempotency - if already confirmed, return existing order
        if (order.getStatus() != Order.OrderStatus.PLACED) {
            log.warn("Order {} already has status: {}. Skipping confirmation.", order.getOrderNumber(), order.getStatus());
            return order;
        }
        
        // Step 3: Lock products again for atomic stock deduction
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toList());
        
        List<Product> lockedProducts = productRepository.findAllByIdWithLock(productIds);
        var productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        
        // Step 4: Validate stock availability before deduction
        StringBuilder stockErrors = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            
            if (product == null) {
                stockErrors.append("Product not found: ").append(item.getProduct().getName()).append(". ");
                continue;
            }
            
            int currentStock = product.getStock();
            if (currentStock < item.getQuantity()) {
                stockErrors.append("Insufficient stock for ").append(product.getName())
                        .append(". Available: ").append(currentStock)
                        .append(", Required: ").append(item.getQuantity()).append(". ");
            }
        }
        
        if (stockErrors.length() > 0) {
            log.error("Stock validation failed for order {}: {}", order.getOrderNumber(), stockErrors.toString());
            // Stock became unavailable - mark order for manual review but keep PENDING status
            throw new RuntimeException("Stock validation failed after payment: " + stockErrors.toString());
        }
        
        // Step 5: Deduct stock atomically
        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            int newStock = product.getStock() - item.getQuantity();
            
            if (newStock < 0) {
                throw new RuntimeException("Stock underflow for product: " + product.getName());
            }
            
            product.setStock(newStock);
            productRepository.save(product);
            log.debug("Deducted stock for product {}: {} units", product.getId(), item.getQuantity());
        }
        
        // Step 6: Update order status and save payment ID
        order.setStatus(Order.OrderStatus.PLACED);
        order.setRazorpayPaymentId(razorpayPaymentId);
        order = orderRepository.save(order);

        // Create initial history entry
        createOrderHistoryEntry(order, Order.OrderStatus.PLACED, "SYSTEM", "Order payment confirmed and stock deducted");

        log.info("Payment confirmed for order: {}", order.getOrderNumber());
        
        // Step 7: Clear user's cart
        Cart cart = cartRepository.findByUserId(order.getUser().getId()).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
            log.debug("Cleared cart for user: {}", order.getUser().getId());
        }
        
        // Step 8: Send confirmation email asynchronously
        try {
            emailService.sendOrderConfirmation(order);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order: {}", order.getOrderNumber(), e);
            // Don't fail transaction if email fails
        }
        
        return order;
    }
    
    /**
     * Legacy Stripe payment confirmation (keeping for backward compatibility if needed)
     */
    @Transactional
    public Order confirmPaymentByStripe(String paymentIntentId) {
        Order order = orderRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Order not found for payment"));
        
        if (order.getStatus() != Order.OrderStatus.PLACED) {
            return order;
        }
        
        List<Long> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toList());
        
        List<Product> lockedProducts = productRepository.findAllByIdWithLock(productIds);
        var productMap = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        
        StringBuilder stockErrors = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            
            if (product == null) {
                stockErrors.append("Product not found: ").append(item.getProduct().getName()).append(". ");
                continue;
            }
            
            int currentStock = product.getStock();
            if (currentStock < item.getQuantity()) {
                stockErrors.append("Insufficient stock for ").append(product.getName())
                        .append(". Available: ").append(currentStock)
                        .append(", Required: ").append(item.getQuantity()).append(". ");
            }
        }
        
        if (stockErrors.length() > 0) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new RuntimeException("Stock validation failed after payment: " + stockErrors.toString());
        }
        
        for (OrderItem item : order.getItems()) {
            Product product = productMap.get(item.getProduct().getId());
            int newStock = product.getStock() - item.getQuantity();
            
            if (newStock < 0) {
                throw new RuntimeException("Stock underflow for product: " + product.getName());
            }
            
            product.setStock(newStock);
            productRepository.save(product);
        }
        
        order.setStatus(Order.OrderStatus.PLACED);
        order = orderRepository.save(order);
        
        Cart cart = cartRepository.findByUserId(order.getUser().getId()).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cartRepository.save(cart);
        }
        
        try {
            emailService.sendOrderConfirmation(order);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email", e);
        }
        
        return order;
    }
    
    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Page<Order> getUserOrdersPage(User user, Pageable pageable) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    public Order getOrderById(Long id, User user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return order;
    }
    
    public Order getOrderByNumber(String orderNumber, User user) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return order;
    }
    
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Order.OrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        order = orderRepository.save(order);

        // Create history entry
        createOrderHistoryEntry(order, status, "SYSTEM", null);

        emailService.sendOrderStatusUpdate(order);

        return order;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status, String updatedBy, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Order.OrderStatus previousStatus = order.getStatus();
        order.setStatus(status);
        order = orderRepository.save(order);

        // Create history entry
        createOrderHistoryEntry(order, status, updatedBy, notes);

        emailService.sendOrderStatusUpdate(order);

        return order;
    }

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

    public List<OrderTimelineResponse> getOrderTimeline(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        List<OrderHistory> historyList = orderHistoryRepository.findByOrderIdOrderByTimestampAsc(orderId);

        return historyList.stream()
                .map(history -> OrderTimelineResponse.builder()
                        .id(history.getId())
                        .status(history.getStatus().toString())
                        .timestamp(history.getTimestamp())
                        .notes(history.getNotes())
                        .updatedBy(history.getUpdatedBy())
                        .isActive(history.getStatus() == order.getStatus())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Transactional
    public Order updateTrackingNumber(Long orderId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setTrackingNumber(trackingNumber);
        order.setStatus(Order.OrderStatus.SHIPPED);
        order = orderRepository.save(order);
        
        // Create history entry
        createOrderHistoryEntry(order, Order.OrderStatus.SHIPPED, "SYSTEM", "Tracking number added: " + trackingNumber);
        
        emailService.sendShippingNotification(order);
        
        return order;
    }
    
    /**
     * Generate unique order number with timestamp and random component
     */
    private String generateOrderNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp + "-" + random;
    }
    
    /**
     * Cancel order if payment hasn't been confirmed
     */
    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = getOrderById(orderId, user);
        
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
        createOrderHistoryEntry(order, Order.OrderStatus.CANCELLED, user.getEmail(), "Order cancelled by user");
        
        return order;
    }
    
    /**
     * Restore stock when order is cancelled
     */
    private void restoreStockForOrder(Order order) {
        log.info("Restoring stock for cancelled order: {}", order.getOrderNumber());
        
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int restoredStock = product.getStock() + item.getQuantity();
            product.setStock(restoredStock);
            productRepository.save(product);
            log.debug("Restored stock for product {}: +{} units (now {})", 
                     product.getId(), item.getQuantity(), restoredStock);
        }
    }
    
    /**
     * Get total count of all orders
     */
    public Long countTotalOrders() {
        return orderRepository.count();
    }
}
