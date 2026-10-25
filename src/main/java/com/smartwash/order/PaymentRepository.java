package com.smartwash.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByIdDesc(Long orderId);

    /** Chặn webhook gửi lặp: cùng một khoá thì chỉ ghi nhận một lần (mục 10.1). */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
