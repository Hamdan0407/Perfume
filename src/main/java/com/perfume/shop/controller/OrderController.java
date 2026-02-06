package com.perfume.shop.controller;

import com.perfume.shop.dto.CheckoutRequest;
import com.perfume.shop.dto.OrderTimelineResponse;
import com.perfume.shop.dto.RazorpayOrderResponse;
import com.perfume.shop.dto.RazorpayPaymentVerificationRequest;
import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.User;
import com.perfume.shop.service.OrderService;
import com.perfume.shop.service.RazorpayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order Controller - Manages order lifecycle and payment processing.
 * 
 * Handles order creation, payment verification, order tracking, and cancellation.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderService orderService;
    private final RazorpayService razorpayService;
    
    /**
     * Create order and initialize Razorpay payment.
     * 
     * Validates cart, locks prices, and creates Razorpay order.
     * Stock is reserved but not deducted until payment is verified.
     * 
     * @param user Authenticated user
     * @param request Checkout request with shipping details
     * @return Razorpay order details for payment processing
     */
    @PostMapping("/checkout")
    public ResponseEntity<RazorpayOrderResponse> createOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CheckoutRequest request
    ) {
        log.info("Creating order for user: {}", user.getEmail());
        return ResponseEntity.ok(orderService.createOrder(user, request));
    }
    
    /**
     * Verify Razorpay payment signature and confirm order.
     * 
     * Called by frontend after successful payment.
     * Verifies payment signature using HMAC SHA256.
     * Atomically deducts stock and updates order status on success.
     * 
     * @param user Authenticated user
     * @param request Payment verification request with Razorpay IDs and signature
     * @return Confirmed order
     * @throws RuntimeException if signature invalid or user doesn't own order
     */
    @PostMapping("/verify-payment")
    public ResponseEntity<Order> verifyPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RazorpayPaymentVerificationRequest request
    ) {
        log.info("Verifying payment for order: {} by user: {}", 
                request.getRazorpayOrderId(), user.getEmail());
        
        // Verify payment signature (HMAC SHA256)
        if (!razorpayService.verifyPaymentSignature(request)) {
            log.error("Payment signature verification failed - Order: {}, Payment: {}", 
                    request.getRazorpayOrderId(), request.getRazorpayPaymentId());
            throw new RuntimeException("Payment verification failed: Invalid signature");
        }
        
        // Confirm payment (atomically deducts stock)
        Order order = orderService.confirmPayment(
                request.getRazorpayOrderId(), 
                request.getRazorpayPaymentId()
        );
        
        // Verify user owns this order
        if (!order.getUser().getId().equals(user.getId())) {
            log.error("Authorization failed - User {} attempted to access order {} owned by user {}", 
                    user.getId(), order.getId(), order.getUser().getId());
            throw new RuntimeException("Access denied: You don't own this order");
        }
        
        log.info("Payment verified successfully for order: {}", order.getId());
        return ResponseEntity.ok(order);
    }
    
    /**
     * Get all orders for authenticated user.
     * 
     * @param user Authenticated user
     * @return List of user's orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(orderService.getUserOrders(user));
    }
    
    /**
     * Get user orders with pagination.
     * 
     * @param user Authenticated user
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @return Page of user's orders
     */
    @GetMapping("/page")
    public ResponseEntity<Page<Order>> getUserOrdersPage(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(orderService.getUserOrdersPage(user, pageable));
    }
    
    /**
     * Get order by ID.
     * 
     * @param id Order ID
     * @param user Authenticated user
     * @return Order details
     * @throws RuntimeException if order not found or user doesn't own it
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(orderService.getOrderById(id, user));
    }
    
    /**
     * Get order by order number.
     * 
     * @param orderNumber Order number (e.g., "ORD-123456")
     * @param user Authenticated user
     * @return Order details
     * @throws RuntimeException if order not found or user doesn't own it
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<Order> getOrderByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber, user));
    }
    
    /**
     * Cancel an order.
     * 
     * Only orders in PLACED or CONFIRMED status can be cancelled.
     * Stock is restored upon cancellation.
     * 
     * @param id Order ID
     * @param user Authenticated user
     * @return Cancelled order
     * @throws RuntimeException if order cannot be cancelled
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        log.info("User {} cancelling order {}", user.getEmail(), id);
        return ResponseEntity.ok(orderService.cancelOrder(id, user));
    }

    /**
     * Get order timeline with status history.
     * 
     * @param id Order ID
     * @param user Authenticated user
     * @return List of timeline entries
     * @throws RuntimeException if order not found or user doesn't own it
     */
    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<OrderTimelineResponse>> getOrderTimeline(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        Order order = orderService.getOrderById(id, user);
        return ResponseEntity.ok(orderService.getOrderTimeline(id));
    }
    
    /**
     * Download invoice PDF for delivered order.
     * 
     * Invoice is only available for delivered orders.
     * 
     * @param id Order ID
     * @param user Authenticated user
     * @return PDF invoice as byte array
     * @throws RuntimeException if order not delivered or user doesn't own it
     */
    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        Order order = orderService.getOrderById(id, user);
        
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            log.warn("Invoice requested for non-delivered order: {}", id);
            throw new RuntimeException("Invoice is only available for delivered orders");
        }
        
        byte[] pdf = orderService.generateInvoicePdf(order);
        log.info("Generated invoice for order: {}", order.getOrderNumber());
        
        return ResponseEntity.ok()
                .header("Content-Disposition", 
                        "attachment; filename=invoice-" + order.getOrderNumber() + ".pdf")
                .header("Content-Type", "application/pdf")
                .body(pdf);
    }
}
