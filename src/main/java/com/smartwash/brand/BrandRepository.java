package com.smartwash.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** BR-02: dò trùng theo khoá chuẩn hoá, không theo tên thô. */
    List<Brand> findByNormalizedName(String normalizedName);

    /** Các brand nghi trùng, trừ chính nó khi đang sửa. */
    @Query("SELECT b FROM Brand b WHERE b.normalizedName = :khoa AND (:boQuaId IS NULL OR b.id <> :boQuaId)")
    List<Brand> nghiTrung(@Param("khoa") String khoa, @Param("boQuaId") Long boQuaId);

    List<Brand> findByStatusOrderByNameAsc(String status);

    long countByStatus(String status);

    @Query("""
           SELECT b FROM Brand b
           WHERE (:tuKhoa IS NULL
                  OR LOWER(b.name)       LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                  OR b.normalizedName    LIKE CONCAT('%', :tuKhoa, '%'))
             AND (:trangThai IS NULL OR b.status = :trangThai)
           ORDER BY b.name ASC
           """)
    Page<Brand> timKiem(@Param("tuKhoa") String tuKhoa,
                        @Param("trangThai") String trangThai,
                        Pageable pageable);

    /** Danh sách brand cho ô chọn khi nộp hồ sơ: chỉ brand đang hoạt động. */
    @Query("SELECT b FROM Brand b WHERE b.status = 'ACTIVE' ORDER BY b.name ASC")
    List<Brand> choChonHoSo();
}
