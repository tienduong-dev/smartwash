package com.smartwash.cart;

import com.smartwash.product.Product;
import com.smartwash.store.Store;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bảng cart_items — một dòng trong giỏ (đặc tả mục 5.7).
 *
 * Dòng giỏ KHÔNG chụp ảnh giá. Khác với order_items (BR-10, phải snapshot để
 * đơn cũ không đổi), giỏ hàng luôn đọc giá hiện tại của sản phẩm: khách để hàng
 * trong giỏ một tuần rồi quay lại thì phải thấy giá hôm nay, không phải giá cũ.
 * Chốt giá xảy ra đúng một lần, lúc bấm đặt hàng.
 */
@Entity
@Table(name = "cart_items")
public class CartItem {

    /** Mỗi dòng không cho đặt quá số này, tránh gõ nhầm 999 cái máy giặt. */
    public static final int SO_LUONG_TOI_DA = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity = 1;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    // ------------------------------------------------------------ tiện ích

    /** Giá áp dụng lúc này — đã tính khuyến mãi nếu đang chạy. */
    public BigDecimal getUnitPrice() {
        return product == null ? BigDecimal.ZERO : product.getEffectivePrice();
    }

    public BigDecimal getSubtotal() {
        return getUnitPrice().multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity));
    }

    public Store getStore() {
        return product == null ? null : product.getStore();
    }

    /** Còn mua được không: sản phẩm đang bán, gian hàng đang hoạt động, đủ tồn. */
    public boolean isPurchasable() {
        return product != null
                && product.isSellable()
                && product.getSellableQuantity() >= (quantity == null ? 0 : quantity);
    }

    /**
     * Lý do dòng này không mua được, để hiện nhãn đỏ ngay dưới tên sản phẩm.
     * Trả null nếu dòng bình thường.
     */
    public String getBlockReason() {
        if (product == null) {
            return "Sản phẩm không còn tồn tại.";
        }
        if (!product.isSellable()) {
            return "Sản phẩm đã ngừng bán hoặc gian hàng đang tạm ngưng.";
        }
        int con = product.getSellableQuantity();
        if (con <= 0) {
            return "Sản phẩm đã hết hàng.";
        }
        if (con < (quantity == null ? 0 : quantity)) {
            return "Chỉ còn " + con + " sản phẩm, hãy giảm số lượng.";
        }
        return null;
    }

    // ------------------------------------------------------------- get/set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cart getCart() { return cart; }
    public void setCart(Cart cart) { this.cart = cart; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDateTime getAddedAt() { return addedAt; }
}
