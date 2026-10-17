package com.smartwash.user;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bảng users.
 *
 * Khác bản trước: KHÔNG còn cột `role` dạng chuỗi. Vai trò nằm ở bảng
 * user_roles (đặc tả mục 9.1), vì một tài khoản có thể vừa là khách hàng
 * vừa là chủ gian hàng.
 */
@Entity
@Table(name = "users")
public class User {

    public static final String ACTIVE               = "ACTIVE";
    public static final String LOCKED               = "LOCKED";
    public static final String PENDING_VERIFICATION = "PENDING_VERIFICATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String avatar;

    @Column(nullable = false)
    private String status = ACTIVE;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    /** Đếm số lần đăng nhập sai để khoá tạm (đặc tả mục 12) */
    @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 30)
    private List<UserRole> userRoles = new ArrayList<>();

    // ---------------------------------------------------------------- quyền

    /** Có vai trò này không. Dùng mã trong lớp Role. */
    public boolean hasRole(String code) {
        if (userRoles == null) {
            return false;
        }
        for (UserRole ur : userRoles) {
            if (ur.getRole() != null && code.equals(ur.getRole().getCode())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }

    /** Chủ hoặc nhân viên gian hàng — chưa xét có Store cụ thể nào hay không. */
    public boolean isSeller() {
        return hasRole(Role.SELLER_OWNER) || hasRole(Role.SELLER_STAFF);
    }

    public boolean isCustomer() {
        return hasRole(Role.CUSTOMER);
    }

    /** Danh sách mã vai trò, dùng để hiển thị và ghi nhật ký. */
    public List<String> getRoleCodes() {
        List<String> codes = new ArrayList<>();
        if (userRoles != null) {
            for (UserRole ur : userRoles) {
                if (ur.getRole() != null) {
                    codes.add(ur.getRole().getCode());
                }
            }
        }
        return codes;
    }

    public String getRoleLabels() {
        if (userRoles == null || userRoles.isEmpty()) {
            return "Chưa có vai trò";
        }
        StringBuilder sb = new StringBuilder();
        for (UserRole ur : userRoles) {
            if (ur.getRole() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ur.getRole().getLabel());
        }
        return sb.toString();
    }

    // -------------------------------------------------------------- trạng thái

    public boolean isActive() {
        return ACTIVE.equals(status) && !isTemporarilyLocked();
    }

    /** Đang bị khoá tạm do đăng nhập sai nhiều lần. */
    public boolean isTemporarilyLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public String getStatusLabel() {
        if (isTemporarilyLocked()) {
            return "Tạm khoá";
        }
        return switch (status == null ? "" : status) {
            case ACTIVE               -> "Đang hoạt động";
            case LOCKED               -> "Đã khoá";
            case PENDING_VERIFICATION -> "Chờ xác minh";
            default                   -> status;
        };
    }

    public String getStatusBadge() {
        if (isTemporarilyLocked()) {
            return "badge-warning";
        }
        return switch (status == null ? "" : status) {
            case ACTIVE               -> "badge-success";
            case LOCKED               -> "badge-danger";
            case PENDING_VERIFICATION -> "badge-warning";
            default                   -> "badge-neutral";
        };
    }

    /** Chữ cái đầu để vẽ ảnh đại diện khi không có ảnh thật. */
    public String getInitial() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        return fullName.trim().substring(0, 1).toUpperCase();
    }

    public String getAvatarUrl() {
        if (avatar == null || avatar.isBlank()) {
            return null;
        }
        if (avatar.startsWith("http://") || avatar.startsWith("https://") || avatar.startsWith("/")) {
            return avatar;
        }
        return "/storage/" + avatar;
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    public Integer getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(Integer failedLoginCount) { this.failedLoginCount = failedLoginCount; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<UserRole> getUserRoles() { return userRoles; }
    public void setUserRoles(List<UserRole> userRoles) { this.userRoles = userRoles; }
}
