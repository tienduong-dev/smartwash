package com.smartwash.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    @EntityGraph(attributePaths = {"actor"})
    List<OrderStatusHistory> findByStoreOrderIdOrderByCreatedAtAscIdAsc(Long storeOrderId);
}
