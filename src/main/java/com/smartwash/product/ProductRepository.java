package com.smartwash.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** BR / mục 9.3: trùng model trong cùng một gian hàng là lỗi, khác gian thì không. */
    boolean existsByStoreIdAndModelCodeIgnoreCase(Long storeId, String modelCode);

    @Query("""
           SELECT COUNT(p) > 0 FROM Product p
           WHERE p.store.id = :storeId
             AND LOWER(p.modelCode) = LOWER(:modelCode)
             AND (:boQuaId IS NULL OR p.id <> :boQuaId)
           """)
    boolean trungModel(@Param("storeId") Long storeId,
                       @Param("modelCode") String modelCode,
                       @Param("boQuaId") Long boQuaId);

    @EntityGraph(attributePaths = {"store", "store.brand", "brand", "category", "inventory"})
    Optional<Product> findDetailById(Long id);

    @EntityGraph(attributePaths = {"store", "store.brand", "brand", "category", "inventory", "images", "specs"})
    Optional<Product> findDetailBySlug(String slug);

    long countByStatus(String status);

    long countByStoreIdAndStatus(Long storeId, String status);

    long countByCategoryId(Long categoryId);

    long countByBrandId(Long brandId);

    /**
     * Tìm kiếm và lọc cho trang khách (đặc tả mục 5.2).
     *
     * Chỉ trả sản phẩm ACTIVE của gian hàng ACTIVE — khách không nên thấy
     * hàng của gian hàng đang tạm ngưng.
     *
     * Mọi tham số null nghĩa là bỏ qua tiêu chí đó.
     */
    @Query("""
           SELECT p FROM Product p
           JOIN p.store s
           JOIN p.brand b
           JOIN p.category c
           WHERE p.status = 'ACTIVE' AND s.status = 'ACTIVE' AND b.status = 'ACTIVE'
             AND (:tuKhoa    IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                                     OR LOWER(p.modelCode) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
             AND (:cacDanhMuc IS NULL OR c.id IN :cacDanhMuc)
             AND (:brandId   IS NULL OR b.id = :brandId)
             AND (:storeId   IS NULL OR s.id = :storeId)
             AND (:kieuCua   IS NULL OR p.doorType = :kieuCua)
             AND (:inverter  IS NULL OR p.isInverter = :inverter)
             AND (:dungTichTu  IS NULL OR p.capacityKg >= :dungTichTu)
             AND (:dungTichDen IS NULL OR p.capacityKg <= :dungTichDen)
             AND (:giaTu     IS NULL OR p.price >= :giaTu)
             AND (:giaDen    IS NULL OR p.price <= :giaDen)
           """)
    Page<Product> timKiemCongKhai(@Param("tuKhoa") String tuKhoa,
                                  @Param("cacDanhMuc") List<Long> cacDanhMuc,
                                  @Param("brandId") Long brandId,
                                  @Param("storeId") Long storeId,
                                  @Param("kieuCua") String kieuCua,
                                  @Param("inverter") Boolean inverter,
                                  @Param("dungTichTu") BigDecimal dungTichTu,
                                  @Param("dungTichDen") BigDecimal dungTichDen,
                                  @Param("giaTu") BigDecimal giaTu,
                                  @Param("giaDen") BigDecimal giaDen,
                                  Pageable pageable);

    /** Danh sách sản phẩm trong khu người bán — thấy cả nháp và bị từ chối. */
    @Query("""
           SELECT p FROM Product p
           LEFT JOIN FETCH p.category
           LEFT JOIN FETCH p.inventory
           WHERE p.store.id = :storeId
             AND (:tuKhoa IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                                  OR LOWER(p.modelCode) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
             AND (:trangThai IS NULL OR p.status = :trangThai)
           ORDER BY p.updatedAt DESC, p.id DESC
           """)
    Page<Product> cuaGianHang(@Param("storeId") Long storeId,
                              @Param("tuKhoa") String tuKhoa,
                              @Param("trangThai") String trangThai,
                              Pageable pageable);

    /** Hàng đợi kiểm duyệt của quản trị viên (mục 7.6). */
    @Query("""
           SELECT p FROM Product p
           LEFT JOIN FETCH p.store
           LEFT JOIN FETCH p.brand
           LEFT JOIN FETCH p.category
           WHERE (:trangThai IS NULL OR p.status = :trangThai)
             AND (:storeId   IS NULL OR p.store.id = :storeId)
             AND (:tuKhoa    IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                                     OR LOWER(p.modelCode) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
           ORDER BY
             CASE WHEN p.status = 'PENDING_REVIEW' THEN 0 ELSE 1 END,
             p.submittedAt ASC, p.id DESC
           """)
    Page<Product> hangDoiDuyet(@Param("tuKhoa") String tuKhoa,
                               @Param("trangThai") String trangThai,
                               @Param("storeId") Long storeId,
                               Pageable pageable);

    /** Sản phẩm nổi bật cho trang chủ. */
    @Query("""
           SELECT p FROM Product p
           JOIN p.store s JOIN p.brand b
           LEFT JOIN FETCH p.store
           WHERE p.status = 'ACTIVE' AND s.status = 'ACTIVE' AND b.status = 'ACTIVE'
           ORDER BY p.soldCount DESC, p.ratingAvg DESC, p.id DESC
           """)
    List<Product> noiBat(Pageable pageable);

    /** Sản phẩm cùng danh mục, trừ chính nó — gợi ý ở trang chi tiết. */
    @Query("""
           SELECT p FROM Product p
           JOIN p.store s JOIN p.brand b
           WHERE p.status = 'ACTIVE' AND s.status = 'ACTIVE' AND b.status = 'ACTIVE'
             AND p.category.id = :categoryId AND p.id <> :boQuaId
           ORDER BY p.soldCount DESC, p.id DESC
           """)
    List<Product> cungDanhMuc(@Param("categoryId") Long categoryId,
                              @Param("boQuaId") Long boQuaId,
                              Pageable pageable);

    /** Khoảng giá thật của hàng đang bán, để dựng thanh lọc giá. */
    @Query("""
           SELECT MIN(p.price), MAX(p.price) FROM Product p
           JOIN p.store s WHERE p.status = 'ACTIVE' AND s.status = 'ACTIVE'
           """)
    List<Object[]> khoangGia();

    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void tangLuotXem(@Param("id") Long id);
}
