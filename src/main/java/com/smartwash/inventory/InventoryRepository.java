package com.smartwash.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

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
