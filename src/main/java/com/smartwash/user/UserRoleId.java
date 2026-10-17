package com.smartwash.user;

import java.io.Serializable;
import java.util.Objects;

/** Khoá chính kép của bảng user_roles. Tên trường phải trùng tên trường @Id của UserRole. */
public class UserRoleId implements Serializable {

    private Long user;
    private Integer role;

    public UserRoleId() {
    }

    public UserRoleId(Long user, Integer role) {
        this.user = user;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserRoleId other)) {
            return false;
        }
        return Objects.equals(user, other.user) && Objects.equals(role, other.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, role);
    }

    public Long getUser() { return user; }
    public void setUser(Long user) { this.user = user; }
    public Integer getRole() { return role; }
    public void setRole(Integer role) { this.role = role; }
}
