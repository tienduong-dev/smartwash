package com.smartwash.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    /**
     * Giỏ kèm sẵn dòng hàng và sản phẩm, dùng cho trang /cart.
     *
     * Chỉ nạp một collection ("items"); mọi thứ sau đó là quan hệ ToOne nên
     * không dính MultipleBagFetchException.
     */
    @EntityGraph(attributePaths = {
            "items", "items.product", "items.product.store", "items.product.brand",
            "items.product.inventory"})
    Optional<Cart> findDetailByUserId(Long userId);

    /** Số máy trong giỏ cho con số nhỏ trên header — không nạp cả giỏ. */
    @Query("""
           SELECT COALESCE(SUM(i.quantity), 0)
           FROM CartItem i
           WHERE i.cart.user.id = :userId
           """)
    int demSoLuong(@Param("userId") Long userId);
}
