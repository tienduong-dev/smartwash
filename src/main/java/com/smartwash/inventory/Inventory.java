package com.smartwash.inventory;

import com.smartwash.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Bảng inventory — tồn kho, 1-1 với products (BR-09).
 *
 * Hai con số tách bạch:
 *   quantity_available — tồn thực trong kho
 *   quantity_reserved  — phần đã giữ chỗ cho đơn chưa hoàn tất
 * Bán được = available − reserved. Giữ chỗ không trừ tồn thực, nên khi đơn bị
 * huỷ chỉ cần nhả reserved, không phải cộng lại kho.
 *
 * Khoá chính chính là product_id, dùng @MapsId để khỏi sinh cột id thừa.
 */
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 0;

    @Column(name = "cost_price", nullable = false)
    private Long costPrice = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Số lượng còn bán được. */
    public int getSellable() {
        int co = quantityAvailable == null ? 0 : quantityAvailable;
        int giu = quantityReserved == null ? 0 : quantityReserved;
        return Math.max(0, co - giu);
    }

    /** Sắp hết hàng theo ngưỡng người bán đặt. Ngưỡng 0 nghĩa là không cảnh báo. */
    public boolean isLow() {
        return minStock != null && minStock > 0 && getSellable() <= minStock;
    }

    public boolean isOutOfStock() {
        return getSellable() <= 0;
    }

    public String getStatusLabel() {
        if (isOutOfStock()) {
            return "Hết hàng";
        }
        return isLow() ? "Sắp hết" : "Còn hàng";
    }

    public String getStatusBadge() {
        if (isOutOfStock()) {
            return "badge-danger";
        }
        return isLow() ? "badge-warning" : "badge-success";
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getQuantityAvailable() { return quantityAvailable; }
    public void setQuantityAvailable(Integer quantityAvailable) { this.quantityAvailable = quantityAvailable; }
    public Integer getQuantityReserved() { return quantityReserved; }
    public void setQuantityReserved(Integer quantityReserved) { this.quantityReserved = quantityReserved; }
    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }
    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
