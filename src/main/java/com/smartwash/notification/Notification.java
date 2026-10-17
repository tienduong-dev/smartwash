package com.smartwash.notification;

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

/** Bảng notifications — thông báo trong hệ thống (đặc tả mục 9.1). */
@Entity
@Table(name = "notifications")
public class Notification {

    public static final String STORE_APPROVED   = "STORE_APPROVED";
    public static final String STORE_REJECTED   = "STORE_REJECTED";
    public static final String STORE_NEEDS_INFO = "STORE_NEEDS_INFO";
    public static final String PRODUCT_APPROVED = "PRODUCT_APPROVED";
    public static final String PRODUCT_REJECTED = "PRODUCT_REJECTED";
    public static final String NEW_ORDER        = "NEW_ORDER";
    public static final String ORDER_UPDATED    = "ORDER_UPDATED";
    public static final String TICKET_REPLIED   = "TICKET_REPLIED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Lob
    private String content;

    private String link;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isUnread() {
        return readAt == null;
    }

    /** Tên biểu tượng để view chọn hình vẽ phù hợp. */
    public String getIconName() {
        return switch (type == null ? "" : type) {
            case STORE_APPROVED, PRODUCT_APPROVED -> "check";
            case STORE_REJECTED, PRODUCT_REJECTED -> "x";
            case STORE_NEEDS_INFO                 -> "info";
            case NEW_ORDER, ORDER_UPDATED         -> "bag";
            case TICKET_REPLIED                   -> "chat";
            default                               -> "bell";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
