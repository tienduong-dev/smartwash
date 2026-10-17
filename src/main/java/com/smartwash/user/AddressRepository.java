package com.smartwash.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByIsDefaultDescIdDesc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    Optional<Address> findFirstByUserIdAndIsDefaultTrue(Long userId);

    long countByUserId(Long userId);

    /** Bỏ cờ mặc định ở mọi địa chỉ khác trước khi đặt cái mới. */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.id <> :giuLai")
    int boMacDinhKhac(@Param("userId") Long userId, @Param("giuLai") Long giuLai);
}
