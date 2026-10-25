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

    /**
     * Chi tiết một đơn con cho khu người bán.
     *
     * Chỉ nạp sẵn "items" — một bag duy nhất. "order" và "store" là quan hệ
     * ToOne nên nạp kèm thoải mái.
     */
    @EntityGraph(attributePaths = {"order", "order.customer", "store", "items"})
    Optional<StoreOrder> findDetailByStoreOrderCode(String storeOrderCode);

    /**
     * Đơn của một gian hàng, lọc theo trạng thái nếu có.
     *
     * Tham số null nghĩa là bỏ qua tiêu chí, cùng quy ước với các truy vấn lọc
     * khác trong dự án.
     */
    @Query("""
           SELECT so FROM StoreOrder so
           JOIN so.order o
           WHERE so.store.id = :storeId
             AND (:trangThai IS NULL OR so.status = :trangThai)
             AND (:tuKhoa IS NULL
                  OR LOWER(so.storeOrderCode) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                  OR LOWER(o.receiverName)    LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                  OR o.receiverPhone          LIKE CONCAT('%', :tuKhoa, '%'))
           ORDER BY so.createdAt DESC, so.id DESC
           """)
    Page<StoreOrder> cuaGianHang(@Param("storeId") Long storeId,
                                 @Param("trangThai") String trangThai,
                                 @Param("tuKhoa") String tuKhoa,
                                 Pageable pageable);

    List<StoreOrder> findByOrderId(Long orderId);

    long countByStoreIdAndStatus(Long storeId, String status);

    /** Số đơn đang cần người bán động tay, để hiện con số trên menu. */
    @Query("""
           SELECT COUNT(so) FROM StoreOrder so
           WHERE so.store.id = :storeId
             AND so.status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'PROCESSING')
           """)
    long demChoXuLy(@Param("storeId") Long storeId);
}
