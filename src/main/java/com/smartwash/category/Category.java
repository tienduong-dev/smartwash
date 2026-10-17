package com.smartwash.category;

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
import jakarta.persistence.Transient;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng categories — danh mục dạng cây qua parent_id (đặc tả mục 4.4).
 *
 * Cây không giới hạn số cấp ở tầng dữ liệu, nhưng giao diện chỉ dựng 2 cấp
 * (cha → con) vì danh mục máy giặt không cần sâu hơn.
 */
@Entity
@Table(name = "categories")
@BatchSize(size = 30)
public class Category {

    public static final String ACTIVE = "ACTIVE";
    public static final String HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, name ASC")
    @BatchSize(size = 30)
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String description;

    private String image;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private String status = ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Số sản phẩm đang bán, do service nạp riêng chứ không map vào bảng. */
    @Transient
    private Long productCount;

    // ------------------------------------------------------------- nghiệp vụ

    public boolean isRoot() {
        return parent == null;
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    /** Đường dẫn breadcrumb: "Máy giặt / Cửa ngang". */
    public String getPath() {
        return parent == null ? name : parent.getName() + " / " + name;
    }

    public String getStatusLabel() {
        return isActive() ? "Hiện" : "Ẩn";
    }

    public String getStatusBadge() {
        return isActive() ? "badge-success" : "badge-neutral";
    }

    public String getImageUrl() {
        if (image == null || image.isBlank()) {
            return null;
        }
        if (image.startsWith("http://") || image.startsWith("https://") || image.startsWith("/")) {
            return image;
        }
        return "/storage/" + image;
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Category getParent() { return parent; }
    public void setParent(Category parent) { this.parent = parent; }
    public List<Category> getChildren() { return children; }
    public void setChildren(List<Category> children) { this.children = children; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getProductCount() { return productCount; }
    public void setProductCount(Long productCount) { this.productCount = productCount; }
}
