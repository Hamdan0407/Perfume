package com.perfume.shop.repository;

import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    List<OrderHistory> findByOrderIdOrderByTimestampDesc(Long orderId);

    @Query("SELECT h FROM OrderHistory h WHERE h.order = :order ORDER BY h.timestamp DESC")
    List<OrderHistory> findByOrderOrderByTimestampDesc(@Param("order") Order order);

    @Query("SELECT h FROM OrderHistory h WHERE h.order.id = :orderId ORDER BY h.timestamp ASC")
    List<OrderHistory> findByOrderIdOrderByTimestampAsc(@Param("orderId") Long orderId);
}