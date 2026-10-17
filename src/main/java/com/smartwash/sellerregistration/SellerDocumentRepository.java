package com.smartwash.sellerregistration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerDocumentRepository extends JpaRepository<SellerDocument, Long> {

    List<SellerDocument> findByRegistrationIdOrderByUploadedAtAsc(Long registrationId);

    Optional<SellerDocument> findByIdAndRegistrationId(Long id, Long registrationId);

    long countByRegistrationId(Long registrationId);

    long countByRegistrationIdAndStatus(Long registrationId, String status);
}
