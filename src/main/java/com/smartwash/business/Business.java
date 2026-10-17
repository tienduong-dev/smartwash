package com.smartwash.business;

import com.smartwash.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Bảng businesses — pháp nhân đứng sau gian hàng (đặc tả mục 4.2, BR-02).
 *
 * Một doanh nghiệp có thể mở nhiều Store, mỗi Store gắn đúng một Brand.
 */
@Entity
@Table(name = "businesses")
public class Business {

    public static final String PENDING  = "PENDING";
    public static final String VERIFIED = "VERIFIED";
    public static final String REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "tax_code")
    private String taxCode;

    private String address;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "representative_phone")
    private String representativePhone;

    @Column(name = "representative_email")
    private String representativeEmail;

    @Column(name = "verification_status", nullable = false)
    private String verificationStatus = PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isVerified() {
        return VERIFIED.equals(verificationStatus);
    }

    public String getStatusLabel() {
        return switch (verificationStatus == null ? "" : verificationStatus) {
            case PENDING  -> "Chờ xác minh";
            case VERIFIED -> "Đã xác minh";
            case REJECTED -> "Từ chối";
            default       -> verificationStatus;
        };
    }

    public String getStatusBadge() {
        return switch (verificationStatus == null ? "" : verificationStatus) {
            case VERIFIED -> "badge-success";
            case REJECTED -> "badge-danger";
            default       -> "badge-warning";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getRepresentativeName() { return representativeName; }
    public void setRepresentativeName(String representativeName) { this.representativeName = representativeName; }
    public String getRepresentativePhone() { return representativePhone; }
    public void setRepresentativePhone(String representativePhone) { this.representativePhone = representativePhone; }
    public String getRepresentativeEmail() { return representativeEmail; }
    public void setRepresentativeEmail(String representativeEmail) { this.representativeEmail = representativeEmail; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public User getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(User verifiedBy) { this.verifiedBy = verifiedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
