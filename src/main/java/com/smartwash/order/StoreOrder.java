package com.smartwash.order;

import com.smartwash.store.Store;
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
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng store_orders — ĐƠN CON của một gian hàng (BR-11).
 *
 * Vì sao phải tách: mỗi gian hàng tự đóng gói, tự giao, tự chịu hoa hồng và tự
 * đổi trạng thái. Nếu gộp chung một đơn thì gian hàng A xác nhận xong nhưng
 * gian hàng B chưa, không biết ghi trạng thái gì cho cả đơn. Tách ra thì mỗi
 * gian chạy theo nhịp của mình, còn trạng thái đơn tổng được SUY RA từ các đơn
 * con (xem {@link OrderService#trangThaiDonTong}).
 *
 * Tiền hoa hồng chốt ngay lúc đặt: tỉ lệ có thể bị quản trị viên đổi sau này,
 * nhưng đơn đã đặt phải giữ nguyên tỉ lệ tại thời điểm giao dịch.
 */
@Entity
@Table(name = "store_orders")
public class StoreOrder {

    public static final String PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String CONFIRMED        = "CONFIRMED";
    public static final String PROCESSING       = "PROCESSING";
    public static final String SHIPPED          = "SHIPPED";
    public static final String DELIVERED        = "DELIVERED";
    public static final String COMPLETED        = "COMPLETED";
    public static final String CANCEL_REQUESTED = "CANCEL_REQUESTED";
    public static final String CANCELLED        = "CANCELLED";
    public static final String RETURN_REQUESTED = "RETURN_REQUESTED";
    public static final String RETURNED         = "RETURNED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_order_code", nullable = false, length = 24)
    private String storeOrderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "items_subtotal", nullable = false)
    private BigDecimal itemsSubtotal = BigDecimal.ZERO;

    @Column(name = "store_discount", nullable = false)
    private BigDecimal storeDiscount = BigDecimal.ZERO;

    @Column(name = "platform_discount_allocated", nullable = false)
    private BigDecimal platformDiscountAllocated = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "commission_rate", nullable = false)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "commission_amount", nullable = false)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "seller_amount", nullable = false)
    private BigDecimal sellerAmount = BigDecimal.ZERO;

    @Column(name = "store_coupon_code", length = 50)
    private String storeCouponCode;

    @Column(nullable = false, length = 30)
    private String status = PENDING_CONFIRMATION;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "storeOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy(clause = "id asc")
    @BatchSize(size = 20)
    private List<OrderItem> items = new ArrayList<>();

    // ------------------------------------------------------------- tiện ích

    public void addItem(OrderItem item) {
        item.setStoreOrder(this);
        items.add(item);
    }

    public int getTotalQuantity() {
        int tong = 0;
        for (OrderItem i : items) {
            tong += i.getQuantity() == null ? 0 : i.getQuantity();
        }
        return tong;
    }

    public String getStoreName() {
        return store == null ? "Gian hàng không xác định" : store.getName();
    }

    /** Đơn đã kết thúc, không còn đổi trạng thái nữa. */
    public boolean isClosed() {
        return COMPLETED.equals(status) || CANCELLED.equals(status) || RETURNED.equals(status);
    }

    /**
     * Phần tồn kho đang bị đơn này giữ chỗ.
     *
     * Đặt hàng thì cộng vào quantity_reserved; huỷ thì trả lại; giao xong thì
     * trừ hẳn khỏi quantity_available (BR-09). Đơn đã đóng thì không giữ gì.
     */
    public boolean isHoldingStock() {
        return !isClosed() && !DELIVERED.equals(status);
    }

    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case PENDING_CONFIRMATION -> "Chờ xác nhận";
            case CONFIRMED        -> "Đã xác nhận";
            case PROCESSING       -> "Đang chuẩn bị hàng";
            case SHIPPED          -> "Đang giao";
            case DELIVERED        -> "Đã giao";
            case COMPLETED        -> "Hoàn tất";
            case CANCEL_REQUESTED -> "Chờ duyệt huỷ";
            case CANCELLED        -> "Đã huỷ";
            case RETURN_REQUESTED -> "Chờ duyệt trả hàng";
            case RETURNED         -> "Đã trả hàng";
            default               -> status;
        };
    }

    public String getStatusBadge() {
        return switch (status == null ? "" : status) {
            case COMPLETED, DELIVERED -> "badge-success";
            case SHIPPED, PROCESSING, CONFIRMED -> "badge-info";
            case CANCEL_REQUESTED, RETURN_REQUESTED -> "badge-warning";
            case CANCELLED, RETURNED -> "badge-danger";
            default -> "badge-neutral";
        };
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStoreOrderCode() { return storeOrderCode; }
    public void setStoreOrderCode(String storeOrderCode) { this.storeOrderCode = storeOrderCode; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public BigDecimal getItemsSubtotal() { return itemsSubtotal; }
    public void setItemsSubtotal(BigDecimal itemsSubtotal) { this.itemsSubtotal = itemsSubtotal; }
    public BigDecimal getStoreDiscount() { return storeDiscount; }
    public void setStoreDiscount(BigDecimal storeDiscount) { this.storeDiscount = storeDiscount; }
    public BigDecimal getPlatformDiscountAllocated() { return platformDiscountAllocated; }
    public void setPlatformDiscountAllocated(BigDecimal v) { this.platformDiscountAllocated = v; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public BigDecimal getSellerAmount() { return sellerAmount; }
    public void setSellerAmount(BigDecimal sellerAmount) { this.sellerAmount = sellerAmount; }
    public String getStoreCouponCode() { return storeCouponCode; }
    public void setStoreCouponCode(String storeCouponCode) { this.storeCouponCode = storeCouponCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
