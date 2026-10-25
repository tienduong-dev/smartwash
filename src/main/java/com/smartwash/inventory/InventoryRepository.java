package com.smartwash.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Khoá dòng tồn kho để đặt hàng (BR-09).
     *
     * PESSIMISTIC_WRITE sinh ra UPDLOCK trên SQL Server: giao dịch nào vào
     * trước thì giữ dòng, giao dịch sau phải chờ. Không có nó thì hai người
     * cùng mua cái cuối cùng sẽ cùng đọc "còn 1", cùng thấy đủ hàng, và cùng
     * đặt được — bán vượt tồn kho (đây là lỗi kinh điển "lost update").
     *
     * Chỉ dùng trong giao dịch đặt hàng. Các chỗ chỉ đọc để hiển thị thì dùng
     * findById thường, khoá ở đó là thừa và làm chậm cả trang danh sách.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> khoaDeDatHang(@Param("productId") Long productId);

    /** Hàng sắp hết của một gian hàng, để cảnh báo trên trang tổng quan. */
    @Query("""
           SELECT i FROM Inventory i
           JOIN FETCH i.product p
           WHERE p.store.id = :storeId
             AND i.minStock > 0
             AND (i.quantityAvailable - i.quantityReserved) <= i.minStock
           ORDER BY (i.quantityAvailable - i.quantityReserved) ASC
           """)
    Page<Inventory> sapHet(@Param("storeId") Long storeId, Pageable pageable);

    @Query("""
           SELECT COUNT(i) FROM Inventory i
           WHERE i.product.store.id = :storeId
             AND i.minStock > 0
             AND (i.quantityAvailable - i.quantityReserved) <= i.minStock
           """)
    long demSapHet(@Param("storeId") Long storeId);

    @Query("""
           SELECT COUNT(i) FROM Inventory i
           WHERE i.product.store.id = :storeId
             AND (i.quantityAvailable - i.quantityReserved) <= 0
           """)
    long demHetHang(@Param("storeId") Long storeId);
}
