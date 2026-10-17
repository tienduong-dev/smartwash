package com.smartwash.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findTop8ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :luc WHERE n.user.id = :userId AND n.readAt IS NULL")
    int danhDauDocHet(@Param("userId") Long userId, @Param("luc") LocalDateTime luc);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :luc WHERE n.id = :id AND n.user.id = :userId AND n.readAt IS NULL")
    int danhDauDoc(@Param("id") Long id, @Param("userId") Long userId, @Param("luc") LocalDateTime luc);
}
