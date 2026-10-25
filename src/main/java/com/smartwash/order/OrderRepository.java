package com.smartwash.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByOrderCode(String orderCode);

    Optional<Order> findByOrderCode(String orderCode);

    /**
     * Danh sách đơn của một khách.
     *
     * KHÔNG nạp sẵn đơn con ở đây: trang danh sách chỉ cần mã đơn, ngày, tổng
     * tiền, trạng thái. Đơn con có @BatchSize(20) nên nếu template có đụng tới
     * thì cũng chỉ thêm một truy vấn cho cả trang, không bị N+1.
     */
    Page<Order> findByCustomerIdOrderByPlacedAtDesc(Long customerId, Pageable pageable);

    /**
     * Chi tiết một đơn.
     *
     * Chỉ nạp sẵn MỘT collection duy nhất là "storeOrders". Không thêm
     * "storeOrders.items" vào đây, dù nó nằm sâu hơn một tầng: giới hạn một
     * bag của Hibernate tính trên CẢ truy vấn chứ không phải trên từng cấp.
     * orders × store_orders × order_items là tích Descartes ba tầng, nên
     * Hibernate ném MultipleBagFetchException ngay lúc dựng câu lệnh.
     *
     * "items" để lazy; nó có @BatchSize(20) nên Hibernate gom tất cả đơn con
     * của đơn này lại và nạp dòng hàng bằng đúng một truy vấn phụ.
     */
    @EntityGraph(attributePaths = {"customer", "storeOrders", "storeOrders.store"})
    Optional<Order> findDetailByOrderCode(String orderCode);

    long countByCustomerId(Long customerId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :userId AND o.status = :status")
    long demTheoTrangThai(@Param("userId") Long userId, @Param("status") String status);
}
