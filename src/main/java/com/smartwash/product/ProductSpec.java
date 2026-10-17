package com.smartwash.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bảng product_specs — thông số phụ, chỉ để hiển thị.
 *
 * Những thông số khách LỌC hoặc SO SÁNH (dung tích, kiểu cửa, tốc độ vắt…)
 * nằm thành cột riêng trong {@link Product}, không để ở đây.
 */
@Entity
@Table(name = "product_specs")
public class ProductSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "spec_key", nullable = false)
    private String key;

    @Column(name = "spec_value", nullable = false)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public ProductSpec() {
    }

    public ProductSpec(String key, String value, int sortOrder) {
        this.key = key;
        this.value = value;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
