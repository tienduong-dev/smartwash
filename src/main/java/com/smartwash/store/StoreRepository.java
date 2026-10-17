package com.smartwash.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = {"brand", "business"})
    Optional<Store> findDetailById(Long id);

    @EntityGraph(attributePaths = {"brand", "business"})
    Optional<Store> findWithBrandBySlug(String slug);

    List<Store> findByBusinessIdOrderByNameAsc(Long businessId);

    List<Store> findByBrandIdAndStatus(Long brandId, String status);

    long countByStatus(String status);

    long countByBrandId(Long brandId);

    /** Các gian hàng mà một người đang được vận hành. */
    @Query("""
           SELECT m.store FROM StoreMember m
           WHERE m.user.id = :userId AND m.status = 'ACTIVE'
           ORDER BY m.store.name ASC
           """)
    List<Store> cuaNguoiDung(@Param("userId") Long userId);

    @Query("""
           SELECT s FROM Store s
           LEFT JOIN FETCH s.brand
           LEFT JOIN FETCH s.business
           WHERE (:tuKhoa IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
             AND (:trangThai IS NULL OR s.status    = :trangThai)
             AND (:brandId   IS NULL OR s.brand.id  = :brandId)
             AND (:loai      IS NULL OR s.storeType = :loai)
           ORDER BY s.createdAt DESC
           """)
    Page<Store> timKiem(@Param("tuKhoa") String tuKhoa,
                        @Param("trangThai") String trangThai,
                        @Param("brandId") Long brandId,
                        @Param("loai") String loai,
                        Pageable pageable);

    /** Danh sách gian hàng hiện cho khách, dùng ở trang "Thương hiệu & gian hàng". */
    @Query("""
           SELECT s FROM Store s
           LEFT JOIN FETCH s.brand
           WHERE s.status = 'ACTIVE'
           ORDER BY s.isVerified DESC, s.ratingAvg DESC, s.name ASC
           """)
    Page<Store> congKhai(Pageable pageable);
}
