package com.smartwash.cart;

import com.smartwash.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng carts — giỏ hàng của một tài khoản (đặc tả mục 5.7).
 *
 * Mỗi người đúng một giỏ: ràng buộc UQ_carts_user trong DDL. Giỏ gắn với tài
 * khoản chứ không gắn với phiên, nên khách đăng nhập máy khác vẫn thấy giỏ cũ.
 *
 * Giỏ KHÔNG giữ chỗ tồn kho. Bỏ hàng vào giỏ mà không mua thì không được chiếm
 * hàng của người khác; việc giữ chỗ chỉ xảy ra lúc đặt đơn (BR-09).
 */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("addedAt ASC, id ASC")
    @BatchSize(size = 30)
    private List<CartItem> items = new ArrayList<>();

    @CreationTimestamp
    @jakarta.persistence.Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @jakarta.persistence.Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ------------------------------------------------------------ tiện ích

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /** Tổng số máy trong giỏ, dùng cho con số nhỏ trên biểu tượng giỏ hàng. */
    public int getTotalQuantity() {
        if (items == null) {
            return 0;
        }
        int tong = 0;
        for (CartItem i : items) {
            tong += i.getQuantity() == null ? 0 : i.getQuantity();
        }
        return tong;
    }

    /** Tạm tính của cả giỏ, chỉ cộng các dòng còn mua được. */
    public BigDecimal getSubtotal() {
        BigDecimal tong = BigDecimal.ZERO;
        if (items == null) {
            return tong;
        }
        for (CartItem i : items) {
            if (i.isPurchasable()) {
                tong = tong.add(i.getSubtotal());
            }
        }
        return tong;
    }

    /** Có dòng nào đang vướng (hết hàng, ngừng bán, gian hàng đóng) không. */
    public boolean hasBlockedItem() {
        if (items == null) {
            return false;
        }
        for (CartItem i : items) {
            if (!i.isPurchasable()) {
                return true;
            }
        }
        return false;
    }

    public void addItem(CartItem item) {
        item.setCart(this);
        items.add(item);
    }

    // ------------------------------------------------------------- get/set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
