package com.smartwash.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    long countByUserId(Long userId);

    @Query("""
           SELECT w FROM Wishlist w
           JOIN FETCH w.product p
           LEFT JOIN FETCH p.store
           WHERE w.user.id = :userId
           ORDER BY w.createdAt DESC
           """)
    Page<Wishlist> cuaNguoiDung(@Param("userId") Long userId, Pageable pageable);
}
