package com.smartwash.audit;

import com.smartwash.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Bảng audit_logs — nhật ký mọi thao tác nhạy cảm (BR-15, đặc tả mục 12.3).
 *
 * Ghi lại: ai làm, làm gì, trên đối tượng nào, giá trị trước và sau, lý do.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    // --- hành động hay dùng, để không gõ chuỗi tự do khắp nơi
    public static final String REGISTRATION_APPROVED = "REGISTRATION_APPROVED";
    public static final String REGISTRATION_REJECTED = "REGISTRATION_REJECTED";
    public static final String STORE_SUSPENDED       = "STORE_SUSPENDED";
    public static final String STORE_REACTIVATED     = "STORE_REACTIVATED";
    public static final String BRAND_VERIFIED        = "BRAND_VERIFIED";
    public static final String BRAND_LOCKED          = "BRAND_LOCKED";
    public static final String ROLE_GRANTED          = "ROLE_GRANTED";
    public static final String ROLE_REVOKED          = "ROLE_REVOKED";
    public static final String USER_LOCKED           = "USER_LOCKED";
    public static final String PRODUCT_APPROVED      = "PRODUCT_APPROVED";
    public static final String PRODUCT_REJECTED      = "PRODUCT_REJECTED";
    public static final String ORDER_STATUS_CHANGED  = "ORDER_STATUS_CHANGED";
    public static final String SETTING_CHANGED       = "SETTING_CHANGED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null khi hành động do hệ thống tự chạy. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Lob
    @Column(name = "old_value")
    private String oldValue;

    @Lob
    @Column(name = "new_value")
    private String newValue;

    private String reason;

    @Column(name = "ip_address")
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public String getActorName() {
        return actor == null ? "Hệ thống" : actor.getFullName();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
