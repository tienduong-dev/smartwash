package com.smartwash.sellerregistration;

import com.smartwash.brand.Brand;
import com.smartwash.business.Business;
import com.smartwash.store.Store;
import com.smartwash.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng seller_registrations — hồ sơ xin mở gian hàng (đặc tả mục 4.2, 6.1, 7.4).
 *
 * Vòng đời: PENDING → UNDER_REVIEW → (NEEDS_MORE_INFO ⇄ UNDER_REVIEW)
 *                                  → APPROVED | REJECTED
 * Người nộp có thể tự huỷ (CANCELLED) khi hồ sơ chưa được duyệt.
 */
@Entity
@Table(name = "seller_registrations")
public class SellerRegistration {

    public static final String PENDING         = "PENDING";
    public static final String UNDER_REVIEW    = "UNDER_REVIEW";
    public static final String NEEDS_MORE_INFO = "NEEDS_MORE_INFO";
    public static final String APPROVED        = "APPROVED";
    public static final String REJECTED        = "REJECTED";
    public static final String CANCELLED       = "CANCELLED";

    public static final String OFFICIAL    = "OFFICIAL";
    public static final String DISTRIBUTOR = "DISTRIBUTOR";
    public static final String RETAILER    = "RETAILER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã hồ sơ in cho người nộp theo dõi, ví dụ HS2026-000123. */
    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id", nullable = false)
    private User applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    /** Brand có sẵn mà người nộp chọn. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_brand_id")
    private Brand requestedBrand;

    /** Hoặc tên brand mới do người nộp đề xuất, khi chưa có trong danh mục. */
    @Column(name = "requested_brand_name")
    private String requestedBrandName;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "store_slug_proposed")
    private String storeSlugProposed;

    @Column(name = "store_type", nullable = false)
    private String storeType = RETAILER;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "applicant_note")
    private String applicantNote;

    @Column(nullable = false)
    private String status = PENDING;

    @Column(name = "review_note")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    /** Store được tạo ra khi hồ sơ được duyệt. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_store_id")
    private Store createdStore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("uploadedAt ASC")
    @BatchSize(size = 20)
    private List<SellerDocument> documents = new ArrayList<>();

    // ------------------------------------------------------------- nghiệp vụ

    /** Hồ sơ đã chốt, không sửa được nữa. */
    public boolean isFinal() {
        return APPROVED.equals(status) || REJECTED.equals(status) || CANCELLED.equals(status);
    }

    /** Admin còn thao tác được không. */
    public boolean isReviewable() {
        return PENDING.equals(status) || UNDER_REVIEW.equals(status) || NEEDS_MORE_INFO.equals(status);
    }

    /** Người nộp còn bổ sung giấy tờ được không. */
    public boolean canSupplement() {
        return NEEDS_MORE_INFO.equals(status) || PENDING.equals(status);
    }

    public boolean canCancel() {
        return !isFinal();
    }

    /** Tên thương hiệu để hiển thị, dù là brand có sẵn hay tên mới đề xuất. */
    public String getBrandDisplay() {
        if (requestedBrand != null) {
            return requestedBrand.getName();
        }
        return requestedBrandName == null ? "(chưa nêu)" : requestedBrandName;
    }

    public boolean isNewBrandProposal() {
        return requestedBrand == null && requestedBrandName != null && !requestedBrandName.isBlank();
    }

    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case PENDING         -> "Chờ tiếp nhận";
            case UNDER_REVIEW    -> "Đang thẩm định";
            case NEEDS_MORE_INFO -> "Cần bổ sung";
            case APPROVED        -> "Đã duyệt";
            case REJECTED        -> "Từ chối";
            case CANCELLED       -> "Đã huỷ";
            default              -> status;
        };
    }

    public String getStatusBadge() {
        return switch (status == null ? "" : status) {
            case APPROVED        -> "badge-success";
            case REJECTED        -> "badge-danger";
            case CANCELLED       -> "badge-neutral";
            case NEEDS_MORE_INFO -> "badge-warning";
            case UNDER_REVIEW    -> "badge-info";
            default              -> "badge-neutral";
        };
    }

    public String getStoreTypeLabel() {
        return switch (storeType == null ? "" : storeType) {
            case OFFICIAL    -> "Gian hàng chính hãng";
            case DISTRIBUTOR -> "Nhà phân phối uỷ quyền";
            case RETAILER    -> "Cửa hàng bán lẻ";
            default          -> storeType;
        };
    }

    public void addDocument(SellerDocument doc) {
        doc.setRegistration(this);
        documents.add(doc);
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public User getApplicant() { return applicant; }
    public void setApplicant(User applicant) { this.applicant = applicant; }
    public Business getBusiness() { return business; }
    public void setBusiness(Business business) { this.business = business; }
    public Brand getRequestedBrand() { return requestedBrand; }
    public void setRequestedBrand(Brand requestedBrand) { this.requestedBrand = requestedBrand; }
    public String getRequestedBrandName() { return requestedBrandName; }
    public void setRequestedBrandName(String requestedBrandName) { this.requestedBrandName = requestedBrandName; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreSlugProposed() { return storeSlugProposed; }
    public void setStoreSlugProposed(String storeSlugProposed) { this.storeSlugProposed = storeSlugProposed; }
    public String getStoreType() { return storeType; }
    public void setStoreType(String storeType) { this.storeType = storeType; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getApplicantNote() { return applicantNote; }
    public void setApplicantNote(String applicantNote) { this.applicantNote = applicantNote; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public Store getCreatedStore() { return createdStore; }
    public void setCreatedStore(Store createdStore) { this.createdStore = createdStore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<SellerDocument> getDocuments() { return documents; }
    public void setDocuments(List<SellerDocument> documents) { this.documents = documents; }
}
