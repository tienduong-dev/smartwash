package com.smartwash.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByTaxCode(String taxCode);

    boolean existsByTaxCode(String taxCode);

    long countByVerificationStatus(String verificationStatus);

    @Query("""
           SELECT b FROM Business b
           WHERE (:tuKhoa IS NULL
                  OR LOWER(b.legalName) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                  OR b.taxCode          LIKE CONCAT('%', :tuKhoa, '%'))
             AND (:trangThai IS NULL OR b.verificationStatus = :trangThai)
           ORDER BY b.createdAt DESC
           """)
    Page<Business> timKiem(@Param("tuKhoa") String tuKhoa,
                           @Param("trangThai") String trangThai,
                           Pageable pageable);
}
