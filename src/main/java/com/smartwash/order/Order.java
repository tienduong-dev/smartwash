package com.smartwash.order;

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
 * Bảng orders — ĐƠN TỔNG, tức một lần bấm đặt hàng của khách (Phụ lục C.1).
 *
 * Khách trả tiền một lần cho cả giỏ, nhưng hàng đến từ nhiều gian hàng khác
 * nhau, nên đơn tổng được tách thành nhiều {@link StoreOrder} — mỗi gian hàng
 * một đơn con (BR-11). Gian hàng chỉ nhìn thấy đơn con của mình.
 *
 * Địa chỉ nhận được CHÉP vào đây chứ không tham chiếu sang bảng addresses:
 * khách sửa hoặc xoá địa chỉ sau này thì đơn cũ vẫn phải giữ nguyên nơi đã
 * giao. Cùng một lý do với snapshot sản phẩm ở {@link OrderItem} (BR-10).
 */
@Entity
@Table(name = "orders")
public class Order {

    // Trạng thái đơn tổng — SUY RA từ các đơn con, không bao giờ sửa tay.
    public static final String PENDING_CONFIRMATION = "PENDING_CONFIRMATION";
    public static final String PROCESSING = "PROCESSING";
    public static final String SHIPPING   = "SHIPPING";
    public static final String DELIVERED  = "DELIVERED";
    public static final String COMPLETED  = "COMPLETED";
    public static final String CANCELLED  = "CANCELLED";
    public static final String RETURNED   = "RETURNED";

    // Hình thức thanh toán — khớp ràng buộc CK_orders_payment_method.
    public static final String COD       = "COD";
    public static final String VNPAY     = "VNPAY";
    public static final String MOMO      = "MOMO";
    public static final String BANK_DEMO = "BANK_DEMO";

    // Trạng thái thanh toán — ĐỘC LẬP với trạng thái giao hàng (BR-12).
    public static final String UNPAID = "UNPAID";
    public static final String PENDING = "PENDING";
    public static final String PAID   = "PAID";
    public static final String FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, length = 20)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // ----------------------------------------------- địa chỉ nhận (đã chép)

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(name = "province_id")
    private Integer provinceId;

    @Column(name = "province_name")
    private String provinceName;

    @Column(name = "district_id")
    private Integer districtId;

    @Column(name = "district_name")
    private String districtName;

    @Column(name = "ward_code", length = 30)
    private String wardCode;

    @Column(name = "ward_name")
    private String wardName;

    @Column(length = 500)
    private String note;

    // ------------------------------------------------------------- tiền

    @Column(name = "items_subtotal", nullable = false)
    private BigDecimal itemsSubtotal = BigDecimal.ZERO;

    @Column(name = "platform_discount", nullable = false)
    private BigDecimal platformDiscount = BigDecimal.ZERO;

    @Column(name = "store_discount_total", nullable = false)
    private BigDecimal storeDiscountTotal = BigDecimal.ZERO;

    @Column(name = "shipping_fee_total", nullable = false)
    private BigDecimal shippingFeeTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "platform_coupon_code", length = 50)
    private String platformCouponCode;

    // ---------------------------------------------------------- trạng thái

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod = COD;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus = UNPAID;

    @Column(nullable = false, length = 30)
    private String status = PENDING_CONFIRMATION;

    @CreationTimestamp
    @Column(name = "placed_at", updatable = false)
    private LocalDateTime placedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy(clause = "id asc")
    @BatchSize(size = 20)
    private List<StoreOrder> storeOrders = new ArrayList<>();

    // ------------------------------------------------------------- tiện ích

    public void addStoreOrder(StoreOrder so) {
        so.setOrder(this);
        storeOrders.add(so);
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder(addressLine == null ? "" : addressLine);
        for (String phan : new String[]{wardName, districtName, provinceName}) {
            if (phan != null && !phan.isBlank()) {
                sb.append(", ").append(phan);
            }
        }
        return sb.toString();
    }

    public int getTotalQuantity() {
        int tong = 0;
        for (StoreOrder so : storeOrders) {
            tong += so.getTotalQuantity();
        }
        return tong;
    }

    public boolean isPaid() {
        return PAID.equals(paymentStatus);
    }

    /**
     * Khách còn tự huỷ được không.
     *
     * Chỉ cho huỷ khi MỌI đơn con còn ở trạng thái chờ xác nhận. Khi một gian
     * hàng đã xác nhận là họ bắt đầu đóng gói, lúc đó muốn huỷ phải qua luồng
     * yêu cầu huỷ để người bán duyệt, không cho tự ý nữa.
     */
    public boolean isCancellableByCustomer() {
        if (storeOrders.isEmpty()) {
            return false;
        }
        for (StoreOrder so : storeOrders) {
            if (!StoreOrder.PENDING_CONFIRMATION.equals(so.getStatus())) {
                return false;
            }
        }
        return true;
    }

    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case PENDING_CONFIRMATION -> "Chờ xác nhận";
            case PROCESSING -> "Đang chuẩn bị";
            case SHIPPING   -> "Đang giao";
            case DELIVERED  -> "Đã giao";
            case COMPLETED  -> "Hoàn tất";
            case CANCELLED  -> "Đã huỷ";
            case RETURNED   -> "Đã trả hàng";
            default         -> status;
        };
    }

    public String getStatusBadge() {
        return switch (status == null ? "" : status) {
            case COMPLETED, DELIVERED -> "badge-success";
            case SHIPPING, PROCESSING -> "badge-info";
            case CANCELLED, RETURNED  -> "badge-danger";
            default                   -> "badge-neutral";
        };
    }

    public String getPaymentMethodLabel() {
        return switch (paymentMethod == null ? "" : paymentMethod) {
            case COD       -> "Thanh toán khi nhận hàng";
            case VNPAY     -> "VNPAY";
            case MOMO      -> "Ví MoMo";
            case BANK_DEMO -> "Chuyển khoản ngân hàng";
            default        -> paymentMethod;
        };
    }

    public String getPaymentStatusLabel() {
        return switch (paymentStatus == null ? "" : paymentStatus) {
            case UNPAID  -> "Chưa thanh toán";
            case PENDING -> "Đang chờ thanh toán";
            case PAID    -> "Đã thanh toán";
            case FAILED  -> "Thanh toán thất bại";
            default      -> paymentStatus;
        };
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public Integer getProvinceId() { return provinceId; }
    public void setProvinceId(Integer provinceId) { this.provinceId = provinceId; }
    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
    public Integer getDistrictId() { return districtId; }
    public void setDistrictId(Integer districtId) { this.districtId = districtId; }
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
    public String getWardCode() { return wardCode; }
    public void setWardCode(String wardCode) { this.wardCode = wardCode; }
    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public BigDecimal getItemsSubtotal() { return itemsSubtotal; }
    public void setItemsSubtotal(BigDecimal itemsSubtotal) { this.itemsSubtotal = itemsSubtotal; }
    public BigDecimal getPlatformDiscount() { return platformDiscount; }
    public void setPlatformDiscount(BigDecimal platformDiscount) { this.platformDiscount = platformDiscount; }
    public BigDecimal getStoreDiscountTotal() { return storeDiscountTotal; }
    public void setStoreDiscountTotal(BigDecimal storeDiscountTotal) { this.storeDiscountTotal = storeDiscountTotal; }
    public BigDecimal getShippingFeeTotal() { return shippingFeeTotal; }
    public void setShippingFeeTotal(BigDecimal shippingFeeTotal) { this.shippingFeeTotal = shippingFeeTotal; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }
    public String getPlatformCouponCode() { return platformCouponCode; }
    public void setPlatformCouponCode(String platformCouponCode) { this.platformCouponCode = platformCouponCode; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public void setPlacedAt(LocalDateTime placedAt) { this.placedAt = placedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<StoreOrder> getStoreOrders() { return storeOrders; }
    public void setStoreOrders(List<StoreOrder> storeOrders) { this.storeOrders = storeOrders; }
}
