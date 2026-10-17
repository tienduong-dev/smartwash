package com.smartwash.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** Bảng settings — cấu hình toàn sàn dạng khoá/giá trị. */
@Entity
@Table(name = "settings")
public class Setting {

    /** Tỷ lệ phí sàn mặc định, phần trăm. Phụ lục D chốt 5% là giá trị minh hoạ. */
    public static final String COMMISSION_RATE    = "commission_rate";
    /** Số ngày được gửi yêu cầu đổi trả sau khi giao thành công. Baseline 7 ngày. */
    public static final String RETURN_WINDOW_DAYS = "return_window_days";
    /** Có bắt buộc Admin duyệt mọi sản phẩm mới không. */
    public static final String PRODUCT_REVIEW_REQUIRED = "product_review_required";
    /** Số Store tối đa mỗi doanh nghiệp; để trống là không giới hạn. */
    public static final String MAX_STORES_PER_BUSINESS = "max_stores_per_business";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true)
    private String key;

    @Column(name = "setting_value")
    private String value;

    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
