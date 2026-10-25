package com.smartwash.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Long> {

    Optional<StoreOrder> findByStoreOrderCode(String storeOrderCode);

    /** Đơn của một gian hàng, cho khu người bán ở giai đoạn sau. */
    @EntityGraph(attributePaths = {"order", "store"})
    Page<StoreOrder> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);

    List<StoreOrder> findByOrderId(Long orderId);

    long countByStoreIdAndStatus(Long storeId, String status);
}
