package com.smartwash.sellerregistration;

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
 * Bảng seller_documents — giấy tờ kèm hồ sơ mở gian hàng (đặc tả mục 12.1).
 *
 * File nằm trong thư mục riêng ngoài webroot, chỉ Admin và chính người nộp
 * tải được qua controller có kiểm tra quyền — KHÔNG phục vụ tĩnh.
 */
@Entity
@Table(name = "seller_documents")
public class SellerDocument {

    public static final String BUSINESS_LICENSE      = "BUSINESS_LICENSE";
    public static final String AUTHORIZATION_LETTER  = "AUTHORIZATION_LETTER";
    public static final String DISTRIBUTION_CONTRACT = "DISTRIBUTION_CONTRACT";
    public static final String ID_CARD               = "ID_CARD";
    public static final String OTHER                 = "OTHER";

    public static final String PENDING            = "PENDING";
    public static final String ACCEPTED           = "ACCEPTED";
    public static final String NEEDS_REPLACEMENT  = "NEEDS_REPLACEMENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private SellerRegistration registration;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(nullable = false)
    private String status = PENDING;

    private String note;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    public String getTypeLabel() {
        return switch (documentType == null ? "" : documentType) {
            case BUSINESS_LICENSE      -> "Giấy phép kinh doanh";
            case AUTHORIZATION_LETTER  -> "Thư uỷ quyền thương hiệu";
            case DISTRIBUTION_CONTRACT -> "Hợp đồng phân phối";
            case ID_CARD               -> "Giấy tờ tuỳ thân người đại diện";
            case OTHER                 -> "Tài liệu khác";
            default                    -> documentType;
        };
    }

    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case ACCEPTED          -> "Hợp lệ";
            case NEEDS_REPLACEMENT -> "Cần nộp lại";
            default                -> "Chờ kiểm tra";
        };
    }

    public String getStatusBadge() {
        return switch (status == null ? "" : status) {
            case ACCEPTED          -> "badge-success";
            case NEEDS_REPLACEMENT -> "badge-danger";
            default                -> "badge-warning";
        };
    }

    /** Kích thước dễ đọc để hiển thị cạnh tên file. */
    public String getSizeLabel() {
        if (sizeBytes == null) {
            return "";
        }
        double kb = sizeBytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.0f KB", kb);
        }
        return String.format("%.1f MB", kb / 1024.0);
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isPdf() {
        return "application/pdf".equals(mimeType);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SellerRegistration getRegistration() { return registration; }
    public void setRegistration(SellerRegistration registration) { this.registration = registration; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
