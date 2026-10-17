package com.smartwash.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(Long userId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.code = :maVaiTro")
    Optional<UserRole> tim(@Param("userId") Long userId, @Param("maVaiTro") String maVaiTro);

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.code = :maVaiTro")
    boolean coVaiTro(@Param("userId") Long userId, @Param("maVaiTro") String maVaiTro);

    void deleteByUserId(Long userId);
}
