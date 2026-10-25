package com.smartwash.order;

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
 * Bảng order_status_history — nhật ký đổi trạng thái đơn con (đặc tả mục 4.6).
 *
 * Mỗi lần trạng thái đổi đều ghi một dòng: từ đâu sang đâu, ai làm, lúc nào.
 * Không có bảng này thì khi khách khiếu nại "đơn của tôi bị huỷ mà không ai
 * báo", hệ thống không trả lời được là ai huỷ và huỷ lúc nào.
 *
 * actor_user_id để NULL khi hệ thống tự đổi (ví dụ tự hoàn tất sau N ngày giao).
 */
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_order_id", nullable = false)
    private StoreOrder storeOrder;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public OrderStatusHistory() {
    }

    public OrderStatusHistory(StoreOrder storeOrder, String fromStatus, String toStatus,
                              User actor, String note) {
        this.storeOrder = storeOrder;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.note = note;
    }

    public String getActorName() {
        return actor == null ? "Hệ thống" : actor.getFullName();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StoreOrder getStoreOrder() { return storeOrder; }
    public void setStoreOrder(StoreOrder storeOrder) { this.storeOrder = storeOrder; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
