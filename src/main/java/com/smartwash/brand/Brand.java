package com.smartwash.brand;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Bảng brands — thương hiệu máy giặt (đặc tả mục 4.3, BR-02).
 *
 * Tên thô KHÔNG unique: hai doanh nghiệp có thể gõ "Electrolux" và
 * "ELECTROLUX ". Dò trùng dựa trên normalized_name do
 * {@link BrandNameNormalizer} sinh ra, Admin là người quyết định gộp hay tách.
 */
@Entity
@Table(name = "brands")
public class Brand {

    public static final String PENDING_VERIFICATION = "PENDING_VERIFICATION";
    public static final String ACTIVE               = "ACTIVE";
    public static final String LOCKED               = "LOCKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Column(nullable = false, unique = true)
    private String slug;

    private String logo;

    @Lob
    private String description;

    private String website;

    private String country;

    @Column(nullable = false)
    private String status = PENDING_VERIFICATION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Đặt tên và tự sinh normalized_name để không bao giờ lệch nhau. */
    public void setNameAndNormalize(String name) {
        this.name = name;
        this.normalizedName = BrandNameNormalizer.normalize(name);
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public boolean isLocked() {
        return LOCKED.equals(status);
    }

    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case PENDING_VERIFICATION -> "Chờ xác minh";
            case ACTIVE               -> "Đang hoạt động";
            case LOCKED               -> "Đã khoá";
            default                   -> status;
        };
    }

    public String getStatusBadge() {
        return switch (status == null ? "" : status) {
            case ACTIVE -> "badge-success";
            case LOCKED -> "badge-danger";
            default     -> "badge-warning";
        };
    }

    public String getLogoUrl() {
        if (logo == null || logo.isBlank()) {
            return "/img/brands/mac-dinh.svg";
        }
        if (logo.startsWith("http://") || logo.startsWith("https://") || logo.startsWith("/")) {
            return logo;
        }
        return "/storage/" + logo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public User getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(User verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
