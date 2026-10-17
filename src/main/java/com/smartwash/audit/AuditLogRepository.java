package com.smartwash.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Lịch sử của một đối tượng cụ thể, ví dụ một hồ sơ mở gian hàng. */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    @Query("""
           SELECT a FROM AuditLog a
           LEFT JOIN FETCH a.actor
           WHERE (:hanhDong  IS NULL OR a.action     = :hanhDong)
             AND (:loaiDt    IS NULL OR a.entityType = :loaiDt)
             AND (:actorId   IS NULL OR a.actor.id   = :actorId)
             AND (:tu        IS NULL OR a.createdAt >= :tu)
             AND (:den       IS NULL OR a.createdAt <= :den)
           ORDER BY a.createdAt DESC
           """)
    Page<AuditLog> timKiem(@Param("hanhDong") String hanhDong,
                           @Param("loaiDt") String loaiDt,
                           @Param("actorId") Long actorId,
                           @Param("tu") LocalDateTime tu,
                           @Param("den") LocalDateTime den,
                           Pageable pageable);

    @Query("SELECT DISTINCT a.action FROM AuditLog a ORDER BY a.action")
    List<String> cacHanhDong();

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a ORDER BY a.entityType")
    List<String> cacLoaiDoiTuong();
}
