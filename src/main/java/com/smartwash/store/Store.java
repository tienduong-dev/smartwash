package com.smartwash.store;

import com.smartwash.brand.Brand;
import com.smartwash.business.Business;
import com.smartwash.sellerregistration.SellerRegistration;
import com.smartwash.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng stores — gian hàng (đặc tả mục 4.3, 6.3, BR-03, BR-04).
 *
 * BR-03: mỗi Store gắn đúng MỘT Brand và không đổi được sau khi duyệt.
 * BR-04: mọi sản phẩm của Store phải thuộc đúng Brand đó — có trigger
 *        TR_products_brand_khop_store canh ở tầng CSDL.
 *
 * Chủ gian hàng không nằm ở đây mà ở store_members (mục 6.10), vì một
 * gian hàng có thể có nhiều người cùng vận hành.
 */
@Entity
@Table(name = "stores")
@BatchSize(size = 20)
public class Store {

    public static final String PENDING_SETUP = "PENDING_SETUP";
    public static final String ACTIVE        = "ACTIVE";
    public static final String SUSPENDED     = "SUSPENDED";
    public static final String CLOSED        = "CLOSED";

    public static final String OFFICIAL    = "OFFICIAL";
    public static final String DISTRIBUTOR = "DISTRIBUTOR";
    public static final String RETAILER    = "RETAILER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    private SellerRegistration registration;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "store_type", nullable = false)
    private String storeType = RETAILER;

    private String logo;

    private String banner;

    @Lob
    private String description;

    private String phone;

    private String email;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Lob
    @Column(name = "return_policy")
    private String returnPolicy;

    @Lob
    @Column(name = "warranty_policy")
    private String warrantyPolicy;

    @Lob
    @Column(name = "shipping_policy")
    private String shippingPolicy;

    @Column(nullable = false)
    private String status = PENDING_SETUP;

    /** Tỷ lệ hoa hồng riêng. Null nghĩa là dùng mức chung của sàn. */
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "suspended_reason")
    private String suspendedReason;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ------------------------------------------------------------- nghiệp vụ

    /** Đang được phép bán hàng. */
    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    /** Khách còn xem được trang gian hàng không. Tạm ngưng vẫn xem được
     *  để tra cứu đơn cũ, nhưng không mua được. */
    public boolean isVisibleToPublic() {
        return ACTIVE.equals(status) || SUSPENDED.equals(status);
    }

    public boolean canSell() {
        return ACTIVE.equals(status) && brand != null && brand.isActive();
    }

    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case PENDING_SETUP -> "Đang hoàn thiện";
            case ACTIVE        -> "Đang hoạt động";
            case SUSPENDED     -> "Tạm ngưng";
            case CLOSED        -> "Đã đóng";
            default            -> status;
        };
    }

    public String getStatusBadge() {
        return switch (status == null ? "" : status) {
            case ACTIVE        -> "badge-success";
            case SUSPENDED     -> "badge-warning";
            case CLOSED        -> "badge-danger";
            default            -> "badge-neutral";
        };
    }

    public String getStoreTypeLabel() {
        return switch (storeType == null ? "" : storeType) {
            case OFFICIAL    -> "Chính hãng";
            case DISTRIBUTOR -> "Phân phối uỷ quyền";
            case RETAILER    -> "Bán lẻ";
            default          -> storeType;
        };
    }

    /** Gian hàng chính hãng được gắn nhãn nổi bật trên trang sản phẩm. */
    public boolean isOfficial() {
        return OFFICIAL.equals(storeType);
    }

    public String getLogoUrl() {
        if (logo == null || logo.isBlank()) {
            return "/img/stores/mac-dinh.svg";
        }
        if (logo.startsWith("http://") || logo.startsWith("https://") || logo.startsWith("/")) {
            return logo;
        }
        return "/storage/" + logo;
    }

    public String getBannerUrl() {
        if (banner == null || banner.isBlank()) {
            return null;
        }
        if (banner.startsWith("http://") || banner.startsWith("https://") || banner.startsWith("/")) {
            return banner;
        }
        return "/storage/" + banner;
    }

    public String getBrandName() {
        return brand == null ? "" : brand.getName();
    }

    public boolean hasReviews() {
        return reviewCount != null && reviewCount > 0;
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Business getBusiness() { return business; }
    public void setBusiness(Business business) { this.business = business; }
    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
    public SellerRegistration getRegistration() { return registration; }
    public void setRegistration(SellerRegistration registration) { this.registration = registration; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getStoreType() { return storeType; }
    public void setStoreType(String storeType) { this.storeType = storeType; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }
    public String getReturnPolicy() { return returnPolicy; }
    public void setReturnPolicy(String returnPolicy) { this.returnPolicy = returnPolicy; }
    public String getWarrantyPolicy() { return warrantyPolicy; }
    public void setWarrantyPolicy(String warrantyPolicy) { this.warrantyPolicy = warrantyPolicy; }
    public String getShippingPolicy() { return shippingPolicy; }
    public void setShippingPolicy(String shippingPolicy) { this.shippingPolicy = shippingPolicy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    public boolean isVerifiedStore() { return Boolean.TRUE.equals(isVerified); }
    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(LocalDateTime suspendedAt) { this.suspendedAt = suspendedAt; }
    public String getSuspendedReason() { return suspendedReason; }
    public void setSuspendedReason(String suspendedReason) { this.suspendedReason = suspendedReason; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
