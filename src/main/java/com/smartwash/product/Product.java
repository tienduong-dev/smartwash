package com.smartwash.product;

import com.smartwash.brand.Brand;
import com.smartwash.category.Category;
import com.smartwash.inventory.Inventory;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng products (đặc tả mục 4.4, 5.2, 5.5, 6.4, 6.5, 6.6, 7.6).
 *
 * Hai nhóm thông số cố ý tách đôi:
 *   - Cột thật (door_type, capacity_kg, spin_speed_rpm…): những gì khách LỌC
 *     và SO SÁNH. Để dạng key/value thì không đánh chỉ mục và không lọc nổi.
 *   - {@link ProductSpec}: phần còn lại, chỉ để hiển thị.
 *
 * BR-04: brand_id luôn bằng brand của Store. Người bán KHÔNG chọn thương hiệu,
 * backend tự gán — xem ProductService. Tầng CSDL còn có trigger chặn cứng.
 */
@Entity
@Table(name = "products")
@BatchSize(size = 20)
public class Product {

    // --- trạng thái kiểm duyệt
    public static final String DRAFT          = "DRAFT";
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String ACTIVE         = "ACTIVE";
    public static final String REJECTED       = "REJECTED";
    public static final String INACTIVE       = "INACTIVE";
    public static final String SUSPENDED      = "SUSPENDED";

    // --- kiểu cửa
    public static final String CUA_TREN  = "cua_tren";
    public static final String CUA_NGANG = "cua_ngang";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "model_code", nullable = false)
    private String modelCode;

    @Column(nullable = false)
    private String name;

    private String slug;

    @Lob
    private String description;

    @Lob
    private String highlights;

    // ------------------------------------------------------------------ giá

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 0)
    private BigDecimal originalPrice;

    @Column(name = "promo_price", precision = 12, scale = 0)
    private BigDecimal promoPrice;

    @Column(name = "promo_start_at")
    private LocalDateTime promoStartAt;

    @Column(name = "promo_end_at")
    private LocalDateTime promoEndAt;

    // ----------------------------------------------------------- kiểm duyệt

    @Column(nullable = false)
    private String status = DRAFT;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // ----------------------------------------------------------------- khác

    @Column(name = "warranty_months", nullable = false)
    private Integer warrantyMonths = 12;

    private String image;

    @Column(name = "sold_count", nullable = false)
    private Integer soldCount = 0;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    // ------------------------------------------------- thông số dùng để lọc

    @Column(name = "door_type")
    private String doorType;

    @Column(name = "capacity_kg", precision = 4, scale = 1)
    private BigDecimal capacityKg;

    @Column(name = "spin_speed_rpm")
    private Integer spinSpeedRpm;

    @Column(name = "is_inverter", nullable = false)
    private Boolean isInverter = false;

    @Column(name = "energy_rating")
    private String energyRating;

    @Column(name = "width_cm", precision = 6, scale = 1)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 6, scale = 1)
    private BigDecimal heightCm;

    @Column(name = "depth_cm", precision = 6, scale = 1)
    private BigDecimal depthCm;

    @Column(name = "weight_kg", precision = 6, scale = 1)
    private BigDecimal weightKg;

    @Column(name = "power_kwh_year", precision = 8, scale = 2)
    private BigDecimal powerKwhYear;

    @Column(name = "water_liter_cycle", precision = 8, scale = 2)
    private BigDecimal waterLiterCycle;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ------------------------------------------------------------- quan hệ

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 20)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 20)
    private List<ProductSpec> specs = new ArrayList<>();

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Inventory inventory;

    // ============================================================== giá bán

    /** Khuyến mãi có đang chạy không (đúng khoảng thời gian và có giá). */
    public boolean isPromoRunning() {
        if (promoPrice == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (promoStartAt != null && now.isBefore(promoStartAt)) {
            return false;
        }
        return promoEndAt == null || !now.isAfter(promoEndAt);
    }

    /** Giá khách thực trả. Mọi nơi tính tiền phải dùng hàm này, không dùng getPrice(). */
    public BigDecimal getEffectivePrice() {
        return isPromoRunning() ? promoPrice : price;
    }

    /** Giá gạch ngang để so sánh, null nếu không có gì để gạch. */
    public BigDecimal getCompareAtPrice() {
        if (isPromoRunning()) {
            return price;
        }
        if (originalPrice != null && price != null && originalPrice.compareTo(price) > 0) {
            return originalPrice;
        }
        return null;
    }

    /** Phần trăm giảm, 0 nếu không giảm. */
    public int getDiscountPercent() {
        BigDecimal goc = getCompareAtPrice();
        BigDecimal ban = getEffectivePrice();
        if (goc == null || ban == null || goc.signum() <= 0 || goc.compareTo(ban) <= 0) {
            return 0;
        }
        return goc.subtract(ban)
                .multiply(BigDecimal.valueOf(100))
                .divide(goc, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    public boolean isOnSale() {
        return getDiscountPercent() > 0;
    }

    // =========================================================== trạng thái

    /** Khách nhìn thấy và mua được. Gian hàng bị tạm ngưng thì cũng không bán. */
    public boolean isSellable() {
        return ACTIVE.equals(status) && store != null && store.canSell();
    }

    /** Người bán còn sửa được không. Đang chờ duyệt thì khoá để tránh tráo hàng. */
    public boolean isEditable() {
        return DRAFT.equals(status) || REJECTED.equals(status)
                || ACTIVE.equals(status) || INACTIVE.equals(status);
    }

    public boolean canSubmit() {
        return DRAFT.equals(status) || REJECTED.equals(status);
    }

    public boolean isAwaitingReview() {
        return PENDING_REVIEW.equals(status);
    }

    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case DRAFT          -> "Bản nháp";
            case PENDING_REVIEW -> "Chờ duyệt";
            case ACTIVE         -> "Đang bán";
            case REJECTED       -> "Bị từ chối";
            case INACTIVE       -> "Ngừng bán";
            case SUSPENDED      -> "Bị gỡ";
            default             -> status;
        };
    }

    public String getStatusBadge() {
        return switch (status == null ? "" : status) {
            case ACTIVE         -> "badge-success";
            case PENDING_REVIEW -> "badge-warning";
            case REJECTED       -> "badge-danger";
            case SUSPENDED      -> "badge-danger";
            case INACTIVE       -> "badge-neutral";
            default             -> "badge-neutral";
        };
    }

    // ================================================================= ảnh

    /** Ảnh đại diện: cột image, hoặc ảnh chính trong danh sách, hoặc ảnh mặc định. */
    public String getImageUrl() {
        if (image != null && !image.isBlank()) {
            return chuanHoaDuongDan(image);
        }
        if (images != null) {
            for (ProductImage pi : images) {
                if (pi.isPrimaryImage()) {
                    return pi.getUrl();
                }
            }
            if (!images.isEmpty()) {
                return images.get(0).getUrl();
            }
        }
        return "/img/products/mac-dinh.svg";
    }

    static String chuanHoaDuongDan(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("/")) {
            return s;
        }
        return "/storage/" + s;
    }

    // ============================================================ thông số

    public String getDoorTypeLabel() {
        return switch (doorType == null ? "" : doorType) {
            case CUA_TREN  -> "Cửa trên";
            case CUA_NGANG -> "Cửa ngang";
            default        -> "—";
        };
    }

    /** "10.5 kg" — bỏ đuôi 0 thừa. */
    public String getCapacityLabel() {
        if (capacityKg == null) {
            return "—";
        }
        return capacityKg.stripTrailingZeros().toPlainString() + " kg";
    }

    public boolean isInverterMachine() {
        return Boolean.TRUE.equals(isInverter);
    }

    /** Kích thước gộp một dòng, null nếu chưa khai đủ. */
    public String getDimensionLabel() {
        if (widthCm == null || heightCm == null || depthCm == null) {
            return null;
        }
        return widthCm.stripTrailingZeros().toPlainString() + " × "
                + heightCm.stripTrailingZeros().toPlainString() + " × "
                + depthCm.stripTrailingZeros().toPlainString() + " cm";
    }

    // ==================================================== chi phí vận hành

    /*
     * Ước tính chi phí điện nước một năm từ thông số nhà sản xuất công bố,
     * hiển thị ở cột phải trang chi tiết để khách so sánh giữa các máy.
     *
     * Đây là con số THAM KHẢO, không phải cam kết: đơn giá điện nước thay đổi
     * theo bậc thang và theo địa phương, số lần giặt mỗi nhà mỗi khác. Giao diện
     * phải ghi rõ giả định kèm theo. Khi có bảng giá thật thì chuyển mấy hằng số
     * này sang bảng settings thay vì sửa code.
     */
    private static final BigDecimal GIA_DIEN_MOI_KWH = new BigDecimal("2000");
    private static final BigDecimal GIA_NUOC_MOI_M3  = new BigDecimal("10000");
    private static final int LAN_GIAT_MOI_NAM = 208;   // 4 lần/tuần × 52 tuần

    public boolean hasRunningCost() {
        return powerKwhYear != null || waterLiterCycle != null;
    }

    /** Tiền điện ước tính mỗi năm, null nếu chưa khai điện năng tiêu thụ. */
    public BigDecimal getYearlyPowerCost() {
        if (powerKwhYear == null) {
            return null;
        }
        return powerKwhYear.multiply(GIA_DIEN_MOI_KWH).setScale(0, RoundingMode.HALF_UP);
    }

    /** Tiền nước ước tính mỗi năm, null nếu chưa khai lượng nước mỗi lần giặt. */
    public BigDecimal getYearlyWaterCost() {
        if (waterLiterCycle == null) {
            return null;
        }
        return waterLiterCycle
                .multiply(BigDecimal.valueOf(LAN_GIAT_MOI_NAM))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)  // lít -> m³
                .multiply(GIA_NUOC_MOI_M3)
                .setScale(0, RoundingMode.HALF_UP);
    }

    /** Tổng điện + nước, bỏ qua phần nào chưa có số liệu. */
    public BigDecimal getYearlyRunningCost() {
        BigDecimal dien = getYearlyPowerCost();
        BigDecimal nuoc = getYearlyWaterCost();
        if (dien == null && nuoc == null) {
            return null;
        }
        return (dien == null ? BigDecimal.ZERO : dien)
                .add(nuoc == null ? BigDecimal.ZERO : nuoc);
    }

    public int getWashesPerYearAssumed() {
        return LAN_GIAT_MOI_NAM;
    }

    // ============================================================== tồn kho

    /** Số lượng bán được = tồn thực trừ phần đang giữ chỗ cho đơn chưa xong. */
    public int getSellableQuantity() {
        return inventory == null ? 0 : inventory.getSellable();
    }

    public boolean isInStock() {
        return getSellableQuantity() > 0;
    }

    public boolean isLowStock() {
        return inventory != null && inventory.isLow();
    }

    public boolean hasReviews() {
        return reviewCount != null && reviewCount > 0;
    }

    // ------------------------------------------------------------- tiện ích

    public void addImage(ProductImage img) {
        img.setProduct(this);
        images.add(img);
    }

    public void addSpec(ProductSpec spec) {
        spec.setProduct(this);
        specs.add(spec);
    }

    public String getStoreName() {
        return store == null ? "" : store.getName();
    }

    public String getBrandName() {
        return brand == null ? "" : brand.getName();
    }

    public String getCategoryName() {
        return category == null ? "" : category.getName();
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public BigDecimal getPromoPrice() { return promoPrice; }
    public void setPromoPrice(BigDecimal promoPrice) { this.promoPrice = promoPrice; }
    public LocalDateTime getPromoStartAt() { return promoStartAt; }
    public void setPromoStartAt(LocalDateTime promoStartAt) { this.promoStartAt = promoStartAt; }
    public LocalDateTime getPromoEndAt() { return promoEndAt; }
    public void setPromoEndAt(LocalDateTime promoEndAt) { this.promoEndAt = promoEndAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getSoldCount() { return soldCount; }
    public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public String getDoorType() { return doorType; }
    public void setDoorType(String doorType) { this.doorType = doorType; }
    public BigDecimal getCapacityKg() { return capacityKg; }
    public void setCapacityKg(BigDecimal capacityKg) { this.capacityKg = capacityKg; }
    public Integer getSpinSpeedRpm() { return spinSpeedRpm; }
    public void setSpinSpeedRpm(Integer spinSpeedRpm) { this.spinSpeedRpm = spinSpeedRpm; }
    public Boolean getIsInverter() { return isInverter; }
    public void setIsInverter(Boolean isInverter) { this.isInverter = isInverter; }
    public String getEnergyRating() { return energyRating; }
    public void setEnergyRating(String energyRating) { this.energyRating = energyRating; }
    public BigDecimal getWidthCm() { return widthCm; }
    public void setWidthCm(BigDecimal widthCm) { this.widthCm = widthCm; }
    public BigDecimal getHeightCm() { return heightCm; }
    public void setHeightCm(BigDecimal heightCm) { this.heightCm = heightCm; }
    public BigDecimal getDepthCm() { return depthCm; }
    public void setDepthCm(BigDecimal depthCm) { this.depthCm = depthCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public BigDecimal getPowerKwhYear() { return powerKwhYear; }
    public void setPowerKwhYear(BigDecimal powerKwhYear) { this.powerKwhYear = powerKwhYear; }
    public BigDecimal getWaterLiterCycle() { return waterLiterCycle; }
    public void setWaterLiterCycle(BigDecimal waterLiterCycle) { this.waterLiterCycle = waterLiterCycle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }
    public List<ProductSpec> getSpecs() { return specs; }
    public void setSpecs(List<ProductSpec> specs) { this.specs = specs; }
    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
}
