package com.smartwash.order;

import com.smartwash.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Bảng order_items — một dòng hàng đã mua (BR-10).
 *
 * MỌI thông tin hiển thị đều là bản chụp tại thời điểm đặt: tên, mã model,
 * thương hiệu, ảnh, đơn giá, số tháng bảo hành. Trang xem lại đơn cũ KHÔNG
 * join sang bảng products.
 *
 * Lý do: người bán đổi giá, đổi tên, gỡ sản phẩm hay cả gian hàng đóng cửa thì
 * hoá đơn đã xuất vẫn phải giữ nguyên nội dung. Nếu đọc giá hiện tại ra hiển
 * thị, khách mở lại đơn tháng trước sẽ thấy một con số khác với số họ đã trả —
 * đó là kịch bản AT-10 trong đặc tả.
 *
 * Cột product_id vẫn giữ (ON DELETE SET NULL) nhưng chỉ dùng để dẫn link sang
 * trang sản phẩm nếu nó còn tồn tại, không dùng để lấy dữ liệu hiển thị.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id", nullable = false)
    private StoreOrder storeOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productName;

    @Column(name = "model_snapshot", length = 100)
    private String modelCode;

    @Column(name = "brand_name_snapshot", length = 150)
    private String brandName;

    @Column(name = "image_snapshot", length = 500)
    private String image;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "discount_allocated", nullable = false)
    private BigDecimal discountAllocated = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "warranty_months_snapshot", nullable = false)
    private Integer warrantyMonths = 0;

    // ------------------------------------------------------------- tiện ích

    /** Ảnh để hiện trong đơn; dùng ảnh đã chụp, không đọc lại từ sản phẩm. */
    public String getImageUrl() {
        if (image == null || image.isBlank()) {
            return "/img/products/mac-dinh.svg";
        }
        if (image.startsWith("http://") || image.startsWith("https://") || image.startsWith("/")) {
            return image;
        }
        return "/storage/" + image;
    }

    /** Sản phẩm còn tồn tại thì cho bấm vào xem lại, không thì chỉ hiện chữ. */
    public String getProductSlug() {
        return product == null ? null : product.getSlug();
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StoreOrder getStoreOrder() { return storeOrder; }
    public void setStoreOrder(StoreOrder storeOrder) { this.storeOrder = storeOrder; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getDiscountAllocated() { return discountAllocated; }
    public void setDiscountAllocated(BigDecimal discountAllocated) { this.discountAllocated = discountAllocated; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public Integer getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(Integer warrantyMonths) { this.warrantyMonths = warrantyMonths; }
}
