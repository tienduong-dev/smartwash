package com.smartwash.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bảng roles — bốn vai trò cố định của hệ thống (đặc tả mục 3.1).
 * Không cho tạo thêm vai trò từ giao diện; ràng buộc CHECK ở database chặn sẵn.
 */
@Entity
@Table(name = "roles")
public class Role {

    public static final String ADMIN        = "ADMIN";
    public static final String SELLER_OWNER = "SELLER_OWNER";
    public static final String SELLER_STAFF = "SELLER_STAFF";
    public static final String CUSTOMER     = "CUSTOMER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Nhãn tiếng Việt để hiển thị */
    public String getLabel() {
        return switch (code == null ? "" : code) {
            case ADMIN        -> "Quản trị viên";
            case SELLER_OWNER -> "Chủ gian hàng";
            case SELLER_STAFF -> "Nhân viên gian hàng";
            case CUSTOMER     -> "Khách hàng";
            default           -> code;
        };
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
