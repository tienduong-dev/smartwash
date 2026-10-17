package com.smartwash.store;

import com.smartwash.audit.AuditLog;
import com.smartwash.audit.AuditService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Messages;
import com.smartwash.notification.Notification;
import com.smartwash.notification.NotificationService;
import com.smartwash.user.Role;
import com.smartwash.user.User;
import com.smartwash.user.UserRepository;
import com.smartwash.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Gian hàng và nhân sự gian hàng (đặc tả mục 6.3, 6.10, BR-03, BR-06). */
@Service
public class StoreService {

    private final StoreRepository storeRepo;
    private final StoreMemberRepository memberRepo;
    private final UserRepository userRepo;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public StoreService(StoreRepository storeRepo,
                        StoreMemberRepository memberRepo,
                        UserRepository userRepo,
                        UserService userService,
                        NotificationService notificationService,
                        AuditService auditService) {
        this.storeRepo = storeRepo;
        this.memberRepo = memberRepo;
        this.userRepo = userRepo;
        this.userService = userService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------- hồ sơ

    @Transactional
    public Store capNhatHoSo(Long storeId, String ten, String moTa, String dienThoai, String email,
                             String diaChiLayHang, String csDoiTra, String csBaoHanh, String csVanChuyen,
                             String logo, String banner) {
        Store s = timHoacBao(storeId);
        if (ten == null || ten.trim().isEmpty()) {
            throw new LoiNghiepVu("name", Messages.required("tên gian hàng"));
        }
        s.setName(ten.trim());
        s.setDescription(moTa);
        s.setPhone(dienThoai);
        s.setEmail(email);
        s.setPickupAddress(diaChiLayHang);
        s.setReturnPolicy(csDoiTra);
        s.setWarrantyPolicy(csBaoHanh);
        s.setShippingPolicy(csVanChuyen);
        if (logo != null) {
            s.setLogo(logo);
        }
        if (banner != null) {
            s.setBanner(banner);
        }
        return storeRepo.save(s);
    }

    // ------------------------------------------------- tạm ngưng / mở lại

    /**
     * Tạm ngưng gian hàng (BR-06). Sản phẩm ẩn khỏi tìm kiếm nhưng đơn hàng
     * đang chạy vẫn phải hoàn tất — tầng đơn hàng không chặn theo trạng thái
     * gian hàng, chỉ chặn tạo đơn mới.
     */
    @Transactional
    public void tamNgung(Long storeId, String lyDo, User admin, String ip) {
        if (lyDo == null || lyDo.trim().isEmpty()) {
            throw new LoiNghiepVu("reason", "Vui lòng nêu lý do tạm ngưng.");
        }
        Store s = timHoacBao(storeId);
        if (Store.CLOSED.equals(s.getStatus())) {
            throw new LoiNghiepVu("Gian hàng đã đóng.");
        }
        String cu = s.getStatus();
        s.setStatus(Store.SUSPENDED);
        s.setSuspendedAt(LocalDateTime.now());
        s.setSuspendedReason(lyDo.trim());
        storeRepo.save(s);

        baoChoChuGianHang(s, Notification.STORE_REJECTED,
                "Gian hàng \"" + s.getName() + "\" đã bị tạm ngưng", lyDo.trim());

        auditService.ghiDoiTrangThai(admin, AuditLog.STORE_SUSPENDED, "Store", storeId,
                cu, Store.SUSPENDED, lyDo.trim(), ip);
    }

    @Transactional
    public void moLai(Long storeId, User admin, String ip) {
        Store s = timHoacBao(storeId);
        String cu = s.getStatus();
        s.setStatus(Store.ACTIVE);
        s.setSuspendedAt(null);
        s.setSuspendedReason(null);
        storeRepo.save(s);

        baoChoChuGianHang(s, Notification.STORE_APPROVED,
                "Gian hàng \"" + s.getName() + "\" đã hoạt động trở lại", null);

        auditService.ghiDoiTrangThai(admin, AuditLog.STORE_REACTIVATED, "Store", storeId,
                cu, Store.ACTIVE, null, ip);
    }

    @Transactional
    public void dong(Long storeId, String lyDo, User admin, String ip) {
        Store s = timHoacBao(storeId);
        String cu = s.getStatus();
        s.setStatus(Store.CLOSED);
        s.setClosedAt(LocalDateTime.now());
        s.setSuspendedReason(lyDo);
        storeRepo.save(s);
        auditService.ghiDoiTrangThai(admin, "STORE_CLOSED", "Store", storeId, cu, Store.CLOSED, lyDo, ip);
    }

    // ------------------------------------------------------------- nhân sự

    /**
     * Thêm nhân viên vào gian hàng. Người được thêm phải đã có tài khoản —
     * hệ thống không tự tạo tài khoản hộ, để tránh lộ email của người khác.
     */
    @Transactional
    public StoreMember themNhanVien(Long storeId, String email, boolean sanPham, boolean donHang,
                                    boolean hoTro, boolean baoCao, User nguoiMoi, String ip) {
        Store s = timHoacBao(storeId);
        User u = userRepo.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(() -> new LoiNghiepVu("email",
                        "Không tìm thấy tài khoản với email này. Người đó cần đăng ký tài khoản trước."));

        memberRepo.findByStoreIdAndUserId(storeId, u.getId()).ifPresent(cu -> {
            if (cu.isActive()) {
                throw new LoiNghiepVu("email", "Người này đã là thành viên của gian hàng.");
            }
        });

        StoreMember m = memberRepo.findByStoreIdAndUserId(storeId, u.getId()).orElseGet(StoreMember::new);
        m.setStore(s);
        m.setUser(u);
        m.setMemberRole(StoreMember.STAFF);
        m.setCanManageProducts(sanPham);
        m.setCanHandleOrders(donHang);
        m.setCanSupportCustomers(hoTro);
        m.setCanViewReports(baoCao);
        m.setStatus(StoreMember.ACTIVE);
        m.setRevokedAt(null);
        m.setInvitedBy(nguoiMoi);
        memberRepo.save(m);

        userService.themVaiTro(u, Role.SELLER_STAFF, nguoiMoi);

        notificationService.gui(u, Notification.STORE_APPROVED,
                "Bạn được thêm vào gian hàng \"" + s.getName() + "\"",
                "Quyền của bạn: " + m.getPermissionSummary(),
                "/seller/" + s.getId() + "/dashboard");

        auditService.ghi(nguoiMoi, "STORE_MEMBER_ADDED", "Store", storeId, null,
                Map.of("userId", String.valueOf(u.getId()), "email", u.getEmail(),
                       "permissions", m.getPermissionSummary()), null, ip);
        return m;
    }

    @Transactional
    public void doiQuyen(Long memberId, boolean sanPham, boolean donHang, boolean hoTro, boolean baoCao,
                         User nguoiThaoTac, String ip) {
        StoreMember m = memberRepo.findById(memberId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy thành viên."));
        if (m.isOwner()) {
            throw new LoiNghiepVu("Chủ gian hàng luôn có toàn quyền, không đổi được.");
        }
        String cu = m.getPermissionSummary();
        m.setCanManageProducts(sanPham);
        m.setCanHandleOrders(donHang);
        m.setCanSupportCustomers(hoTro);
        m.setCanViewReports(baoCao);
        memberRepo.save(m);
        auditService.ghi(nguoiThaoTac, "STORE_MEMBER_PERMISSIONS", "Store", m.getStore().getId(),
                Map.of("permissions", cu), Map.of("permissions", m.getPermissionSummary()), null, ip);
    }

    @Transactional
    public void thuHoi(Long memberId, User nguoiThaoTac, String ip) {
        StoreMember m = memberRepo.findById(memberId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy thành viên."));
        if (m.isOwner()) {
            throw new LoiNghiepVu("Không thể thu hồi quyền của chủ gian hàng.");
        }
        m.setStatus(StoreMember.REVOKED);
        m.setRevokedAt(LocalDateTime.now());
        memberRepo.save(m);
        auditService.ghi(nguoiThaoTac, "STORE_MEMBER_REVOKED", "Store", m.getStore().getId(),
                Map.of("userId", String.valueOf(m.getUser().getId())), null, null, ip);
    }

    public List<StoreMember> nhanSu(Long storeId) {
        return memberRepo.findByStoreIdOrderByMemberRoleAscJoinedAtAsc(storeId);
    }

    // ------------------------------------------------------------- tra cứu

    public Store timHoacBao(Long id) {
        return storeRepo.findDetailById(id).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy gian hàng."));
    }

    public Store theoSlug(String slug) {
        return storeRepo.findWithBrandBySlug(slug)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy gian hàng."));
    }

    public List<Store> cuaNguoiDung(Long userId) {
        return storeRepo.cuaNguoiDung(userId);
    }

    public Page<Store> danhSach(String tuKhoa, String trangThai, Long brandId, String loai,
                                int trang, int moiTrang) {
        return storeRepo.timKiem(
                tuKhoa == null || tuKhoa.isBlank() ? null : tuKhoa.trim(),
                trangThai == null || trangThai.isBlank() ? null : trangThai,
                brandId,
                loai == null || loai.isBlank() ? null : loai,
                PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    public Page<Store> congKhai(int trang, int moiTrang) {
        return storeRepo.congKhai(PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    private void baoChoChuGianHang(Store s, String loai, String tieuDe, String noiDung) {
        for (StoreMember m : memberRepo.chuGianHang(s.getId())) {
            notificationService.gui(m.getUser(), loai, tieuDe, noiDung, "/seller/" + s.getId() + "/dashboard");
        }
    }
}
