package com.smartwash.user;

import com.smartwash.audit.AuditLog;
import com.smartwash.audit.AuditService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Messages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Tài khoản và vai trò (đặc tả mục 4.1, 9.1, BR-01, BR-15). */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------- đăng ký

    /** Đăng ký khách hàng mới. Mọi tài khoản mới đều nhận vai trò CUSTOMER. */
    @Transactional
    public User dangKy(String hoTen, String email, String soDienThoai, String matKhau) {
        String e = email == null ? "" : email.trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(e)) {
            throw new LoiNghiepVu("email", Messages.unique("email"));
        }
        if (soDienThoai != null && !soDienThoai.isBlank() && userRepository.existsByPhone(soDienThoai.trim())) {
            throw new LoiNghiepVu("phone", Messages.unique("số điện thoại"));
        }

        User u = new User();
        u.setFullName(hoTen == null ? "" : hoTen.trim());
        u.setEmail(e);
        u.setPhone(soDienThoai == null || soDienThoai.isBlank() ? null : soDienThoai.trim());
        u.setPasswordHash(passwordEncoder.encode(matKhau));
        u.setStatus(User.ACTIVE);
        userRepository.save(u);

        themVaiTro(u, Role.CUSTOMER, null);
        return u;
    }

    // ------------------------------------------------------------- vai trò

    /**
     * Cấp vai trò. Gọi lại nhiều lần không sao: đã có thì bỏ qua.
     *
     * @param nguoiCap ai cấp, null nghĩa là hệ thống tự cấp lúc đăng ký
     */
    @Transactional
    public void themVaiTro(User user, String maVaiTro, User nguoiCap) {
        if (userRoleRepository.coVaiTro(user.getId(), maVaiTro)) {
            return;
        }
        Role role = roleRepository.findByCode(maVaiTro)
                .orElseThrow(() -> new LoiNghiepVu("Vai trò không tồn tại: " + maVaiTro));

        UserRole ur = new UserRole();
        ur.setUser(user);
        ur.setRole(role);
        ur.setGrantedBy(nguoiCap);
        ur.setGrantedAt(LocalDateTime.now());
        userRoleRepository.save(ur);
        user.getUserRoles().add(ur);
    }

    @Transactional
    public void boVaiTro(User user, String maVaiTro, User nguoiThaoTac, String ip) {
        userRoleRepository.tim(user.getId(), maVaiTro).ifPresent(ur -> {
            userRoleRepository.delete(ur);
            user.getUserRoles().removeIf(x -> x.getRole() != null && maVaiTro.equals(x.getRole().getCode()));
            auditService.ghi(nguoiThaoTac, AuditLog.ROLE_REVOKED, "User", user.getId(),
                    Map.of("role", maVaiTro), null, null, ip);
        });
    }

    // ---------------------------------------------------------- khoá / mở khoá

    @Transactional
    public void khoa(Long userId, String lyDo, User nguoiThaoTac, String ip) {
        User u = timHoacBao(userId);
        if (u.isAdmin()) {
            throw new LoiNghiepVu("Không thể khoá tài khoản quản trị.");
        }
        if (nguoiThaoTac != null && nguoiThaoTac.getId().equals(userId)) {
            throw new LoiNghiepVu("Không thể tự khoá tài khoản của chính mình.");
        }
        String cu = u.getStatus();
        u.setStatus(User.LOCKED);
        userRepository.save(u);
        auditService.ghiDoiTrangThai(nguoiThaoTac, AuditLog.USER_LOCKED, "User", userId, cu, User.LOCKED, lyDo, ip);
    }

    @Transactional
    public void moKhoa(Long userId, User nguoiThaoTac, String ip) {
        User u = timHoacBao(userId);
        String cu = u.getStatus();
        u.setStatus(User.ACTIVE);
        u.setFailedLoginCount(0);
        u.setLockedUntil(null);
        userRepository.save(u);
        auditService.ghiDoiTrangThai(nguoiThaoTac, "USER_UNLOCKED", "User", userId, cu, User.ACTIVE, null, ip);
    }

    // ------------------------------------------------------------- hồ sơ

    @Transactional
    public void capNhatHoSo(User user, String hoTen, String soDienThoai, String anhDaiDien) {
        if (hoTen == null || hoTen.trim().isEmpty()) {
            throw new LoiNghiepVu("fullName", Messages.required("họ tên"));
        }
        String sdt = soDienThoai == null || soDienThoai.isBlank() ? null : soDienThoai.trim();
        if (sdt != null && !sdt.matches("^0\\d{9}$")) {
            throw new LoiNghiepVu("phone", Messages.phone("số điện thoại"));
        }
        if (sdt != null && !sdt.equals(user.getPhone())) {
            userRepository.findByPhone(sdt).ifPresent(khac -> {
                if (!khac.getId().equals(user.getId())) {
                    throw new LoiNghiepVu("phone", Messages.unique("số điện thoại"));
                }
            });
        }
        user.setFullName(hoTen.trim());
        user.setPhone(sdt);
        if (anhDaiDien != null) {
            user.setAvatar(anhDaiDien);
        }
        userRepository.save(user);
    }

    @Transactional
    public void doiMatKhau(User user, String matKhauCu, String matKhauMoi) {
        if (!passwordEncoder.matches(matKhauCu == null ? "" : matKhauCu, user.getPasswordHash())) {
            throw new LoiNghiepVu("currentPassword", "Mật khẩu hiện tại không đúng.");
        }
        if (matKhauMoi == null || matKhauMoi.length() < 8) {
            throw new LoiNghiepVu("newPassword", Messages.min("mật khẩu mới", 8));
        }
        user.setPasswordHash(passwordEncoder.encode(matKhauMoi));
        userRepository.save(user);
    }

    @Transactional
    public void datLaiMatKhau(User user, String matKhauMoi) {
        user.setPasswordHash(passwordEncoder.encode(matKhauMoi));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    // ------------------------------------------------------------- tra cứu

    public User timHoacBao(Long id) {
        return userRepository.findWithRolesById(id)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy tài khoản."));
    }

    public Page<User> danhSach(String tuKhoa, String trangThai, String vaiTro, int trang, int moiTrang) {
        return userRepository.timKiem(
                rong(tuKhoa), rong(trangThai), rong(vaiTro),
                PageRequest.of(Math.max(0, trang - 1), moiTrang, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<Role> cacVaiTro() {
        return roleRepository.findAll();
    }

    private String rong(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
