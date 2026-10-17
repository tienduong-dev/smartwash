package com.smartwash.notification;

import com.smartwash.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Thông báo trong hệ thống (đặc tả mục 9.1). */
@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Notification gui(User nguoiNhan, String loai, String tieuDe, String noiDung, String lienKet) {
        Notification n = new Notification();
        n.setUser(nguoiNhan);
        n.setType(loai);
        n.setTitle(tieuDe);
        n.setContent(noiDung);
        n.setLink(lienKet);
        return repo.save(n);
    }

    /** Gửi cùng một thông báo cho nhiều người, ví dụ mọi chủ gian hàng. */
    @Transactional
    public void guiNhieu(List<User> nguoiNhan, String loai, String tieuDe, String noiDung, String lienKet) {
        for (User u : nguoiNhan) {
            gui(u, loai, tieuDe, noiDung, lienKet);
        }
    }

    public List<Notification> moiNhat(Long userId) {
        return repo.findTop8ByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<Notification> danhSach(Long userId, int trang, int moiTrang) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    public long soChuaDoc(Long userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void danhDauDoc(Long id, Long userId) {
        repo.danhDauDoc(id, userId, LocalDateTime.now());
    }

    @Transactional
    public int danhDauDocHet(Long userId) {
        return repo.danhDauDocHet(userId, LocalDateTime.now());
    }
}
