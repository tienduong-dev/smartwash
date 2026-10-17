package com.smartwash.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Dùng khi đăng nhập: nạp luôn vai trò để khỏi truy vấn thêm. */
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findWithRolesById(Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByPhone(String phone);

    /**
     * Tìm kiếm cho trang quản trị tài khoản.
     * Mọi tham số đều cho phép null nghĩa là "không lọc theo tiêu chí này".
     */
    @Query("""
           SELECT DISTINCT u FROM User u
           LEFT JOIN u.userRoles ur
           LEFT JOIN ur.role r
           WHERE (:tuKhoa IS NULL
                  OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                  OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                  OR u.phone           LIKE CONCAT('%', :tuKhoa, '%'))
             AND (:trangThai IS NULL OR u.status = :trangThai)
             AND (:vaiTro    IS NULL OR r.code   = :vaiTro)
           """)
    Page<User> timKiem(@Param("tuKhoa") String tuKhoa,
                       @Param("trangThai") String trangThai,
                       @Param("vaiTro") String vaiTro,
                       Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :tu")
    long demTaoTu(@Param("tu") LocalDateTime tu);

    @Query("""
           SELECT u FROM User u
           JOIN u.userRoles ur JOIN ur.role r
           WHERE r.code = :maVaiTro
           """)
    List<User> theoVaiTro(@Param("maVaiTro") String maVaiTro);

    long countByStatus(String status);
}
