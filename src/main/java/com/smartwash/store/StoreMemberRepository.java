package com.smartwash.store;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreMemberRepository extends JpaRepository<StoreMember, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<StoreMember> findByStoreIdOrderByMemberRoleAscJoinedAtAsc(Long storeId);

    Optional<StoreMember> findByStoreIdAndUserId(Long storeId, Long userId);

    @Query("""
           SELECT m FROM StoreMember m
           WHERE m.store.id = :storeId AND m.user.id = :userId AND m.status = 'ACTIVE'
           """)
    Optional<StoreMember> timDangLamViec(@Param("storeId") Long storeId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"store", "store.brand"})
    List<StoreMember> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT COUNT(m) > 0 FROM StoreMember m WHERE m.store.id = :storeId AND m.user.id = :userId AND m.status = 'ACTIVE'")
    boolean laThanhVien(@Param("storeId") Long storeId, @Param("userId") Long userId);

    @Query("""
           SELECT COUNT(m) > 0 FROM StoreMember m
           WHERE m.store.id = :storeId AND m.user.id = :userId
             AND m.status = 'ACTIVE' AND m.memberRole = 'OWNER'
           """)
    boolean laChuGianHang(@Param("storeId") Long storeId, @Param("userId") Long userId);

    long countByStoreIdAndStatus(Long storeId, String status);

    /** Chủ gian hàng đầu tiên — dùng khi cần gửi thông báo cho gian hàng. */
    @Query("""
           SELECT m FROM StoreMember m JOIN FETCH m.user
           WHERE m.store.id = :storeId AND m.memberRole = 'OWNER' AND m.status = 'ACTIVE'
           ORDER BY m.joinedAt ASC
           """)
    List<StoreMember> chuGianHang(@Param("storeId") Long storeId);
}
