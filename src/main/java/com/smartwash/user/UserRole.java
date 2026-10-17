package com.smartwash.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Bảng user_roles. Dùng entity riêng thay cho @ManyToMany vì bảng nối còn
 * lưu AI cấp quyền và cấp lúc nào — BR-15 yêu cầu truy vết được việc đổi quyền.
 */
@Entity
@Table(name = "user_roles")
@IdClass(UserRoleId.class)
public class UserRole {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    public UserRole() {
    }

    public UserRole(User user, Role role, User grantedBy) {
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
    public User getGrantedBy() { return grantedBy; }
    public void setGrantedBy(User grantedBy) { this.grantedBy = grantedBy; }
}
