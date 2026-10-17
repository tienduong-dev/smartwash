package com.smartwash.store;

import com.smartwash.user.User;
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

import java.time.LocalDateTime;

/**
 * Bảng store_members — ai được vận hành gian hàng nào (đặc tả mục 6.10).
 *
 * Thay cho cột shops.user_id của bản cũ: một gian hàng nhiều người, và
 * mỗi người có bộ quyền riêng. OWNER luôn có đủ bốn quyền.
 */
@Entity
@Table(name = "store_members")
public class StoreMember {

    public static final String OWNER = "OWNER";
    public static final String STAFF = "STAFF";

    public static final String ACTIVE  = "ACTIVE";
    public static final String REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "member_role", nullable = false)
    private String memberRole = STAFF;

    @Column(name = "can_manage_products", nullable = false)
    private Boolean canManageProducts = false;

    @Column(name = "can_handle_orders", nullable = false)
    private Boolean canHandleOrders = false;

    @Column(name = "can_support_customers", nullable = false)
    private Boolean canSupportCustomers = false;

    @Column(name = "can_view_reports", nullable = false)
    private Boolean canViewReports = false;

    @Column(nullable = false)
    private String status = ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // ------------------------------------------------------------- nghiệp vụ

    public boolean isOwner() {
        return OWNER.equals(memberRole);
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    /** Bật đủ bốn quyền — dùng khi tạo chủ gian hàng lúc duyệt hồ sơ. */
    public void grantAllPermissions() {
        this.canManageProducts   = true;
        this.canHandleOrders     = true;
        this.canSupportCustomers = true;
        this.canViewReports      = true;
    }

    /** Chủ gian hàng mặc nhiên có mọi quyền, không phụ thuộc bốn cột cờ. */
    public boolean mayManageProducts() {
        return isActive() && (isOwner() || Boolean.TRUE.equals(canManageProducts));
    }

    public boolean mayHandleOrders() {
        return isActive() && (isOwner() || Boolean.TRUE.equals(canHandleOrders));
    }

    public boolean maySupportCustomers() {
        return isActive() && (isOwner() || Boolean.TRUE.equals(canSupportCustomers));
    }

    public boolean mayViewReports() {
        return isActive() && (isOwner() || Boolean.TRUE.equals(canViewReports));
    }

    public String getRoleLabel() {
        return isOwner() ? "Chủ gian hàng" : "Nhân viên";
    }

    public String getStatusLabel() {
        return isActive() ? "Đang làm việc" : "Đã thu hồi";
    }

    public String getStatusBadge() {
        return isActive() ? "badge-success" : "badge-neutral";
    }

    /** Tóm tắt quyền để hiện trong bảng danh sách nhân sự. */
    public String getPermissionSummary() {
        if (isOwner()) {
            return "Toàn quyền";
        }
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(canManageProducts))   { sb.append("Sản phẩm"); }
        if (Boolean.TRUE.equals(canHandleOrders))     { if (sb.length() > 0) sb.append(", "); sb.append("Đơn hàng"); }
        if (Boolean.TRUE.equals(canSupportCustomers)) { if (sb.length() > 0) sb.append(", "); sb.append("Hỗ trợ"); }
        if (Boolean.TRUE.equals(canViewReports))      { if (sb.length() > 0) sb.append(", "); sb.append("Báo cáo"); }
        return sb.length() == 0 ? "Chưa cấp quyền" : sb.toString();
    }

    // ------------------------------------------------------------- get / set

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
    public Boolean getCanManageProducts() { return canManageProducts; }
    public void setCanManageProducts(Boolean canManageProducts) { this.canManageProducts = canManageProducts; }
    public Boolean getCanHandleOrders() { return canHandleOrders; }
    public void setCanHandleOrders(Boolean canHandleOrders) { this.canHandleOrders = canHandleOrders; }
    public Boolean getCanSupportCustomers() { return canSupportCustomers; }
    public void setCanSupportCustomers(Boolean canSupportCustomers) { this.canSupportCustomers = canSupportCustomers; }
    public Boolean getCanViewReports() { return canViewReports; }
    public void setCanViewReports(Boolean canViewReports) { this.canViewReports = canViewReports; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public User getInvitedBy() { return invitedBy; }
    public void setInvitedBy(User invitedBy) { this.invitedBy = invitedBy; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }
}
