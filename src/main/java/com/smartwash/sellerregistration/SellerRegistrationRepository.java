package com.smartwash.sellerregistration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SellerRegistrationRepository extends JpaRepository<SellerRegistration, Long> {

    Optional<SellerRegistration> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"applicant", "business", "requestedBrand", "reviewedBy", "documents"})
    Optional<SellerRegistration> findDetailById(Long id);

    List<SellerRegistration> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    long countByStatus(String status);

    /** Một người không nên có hai hồ sơ đang chờ cùng lúc (mục 6.1). */
    @Query("""
           SELECT COUNT(r) > 0 FROM SellerRegistration r
           WHERE r.applicant.id = :userId
             AND r.status IN ('PENDING', 'UNDER_REVIEW', 'NEEDS_MORE_INFO')
           """)
    boolean dangCoHoSoCho(@Param("userId") Long userId);

    @Query("""
           SELECT r FROM SellerRegistration r
           LEFT JOIN FETCH r.applicant
           LEFT JOIN FETCH r.requestedBrand
           WHERE (:tuKhoa IS NULL
                  OR LOWER(r.storeName) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                  OR r.code             LIKE CONCAT('%', :tuKhoa, '%'))
             AND (:trangThai IS NULL OR r.status    = :trangThai)
             AND (:loaiStore IS NULL OR r.storeType = :loaiStore)
           ORDER BY r.createdAt DESC
           """)
    Page<SellerRegistration> timKiem(@Param("tuKhoa") String tuKhoa,
                                     @Param("trangThai") String trangThai,
                                     @Param("loaiStore") String loaiStore,
                                     Pageable pageable);

    /** Số thứ tự kế tiếp để sinh mã hồ sơ trong năm. */
    @Query("SELECT COUNT(r) FROM SellerRegistration r WHERE r.code LIKE CONCAT(:tienTo, '%')")
    long demTheoTienTo(@Param("tienTo") String tienTo);
}
