package com.smartwash.audit;

import com.smartwash.common.JsonUtil;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Ghi nhật ký thao tác (BR-15, đặc tả mục 12.3).
 *
 * Ghi trong CÙNG giao dịch với hành động được ghi: nếu hành động bị cuộn lại
 * thì dòng nhật ký cũng biến mất, tránh nhật ký nói một đằng dữ liệu một nẻo.
 */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog ghi(User actor, String hanhDong, String loaiDoiTuong, Long doiTuongId,
                        Object giaTriCu, Object giaTriMoi, String lyDo, String ip) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(hanhDong);
        log.setEntityType(loaiDoiTuong);
        log.setEntityId(doiTuongId);
        log.setOldValue(chuyenJson(giaTriCu));
        log.setNewValue(chuyenJson(giaTriMoi));
        log.setReason(lyDo);
        log.setIpAddress(ip);
        return repo.save(log);
    }

    /** Dạng rút gọn: chỉ đổi trạng thái. */
    @Transactional(propagation = Propagation.MANDATORY)
    public AuditLog ghiDoiTrangThai(User actor, String hanhDong, String loaiDoiTuong, Long doiTuongId,
                                    String trangThaiCu, String trangThaiMoi, String lyDo, String ip) {
        return ghi(actor, hanhDong, loaiDoiTuong, doiTuongId,
                Map.of("status", trangThaiCu == null ? "" : trangThaiCu),
                Map.of("status", trangThaiMoi == null ? "" : trangThaiMoi),
                lyDo, ip);
    }

    private String chuyenJson(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof String s) {
            return s;
        }
        return JsonUtil.write(v);
    }

    /** Lấy IP thật kể cả khi chạy sau reverse proxy. */
    public static String ipCua(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int phay = xff.indexOf(',');
            String ip = (phay > 0 ? xff.substring(0, phay) : xff).trim();
            return ip.length() > 45 ? ip.substring(0, 45) : ip;
        }
        String ip = request.getRemoteAddr();
        return ip == null || ip.length() <= 45 ? ip : ip.substring(0, 45);
    }
}
