package com.smartwash.auth;

import com.smartwash.common.LoiNghiepVu;
import com.smartwash.user.User;
import com.smartwash.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Đăng nhập, đăng xuất, lấy người dùng hiện tại.
 *
 * Phiên lưu ở HttpSession, khoá USER_ID. Mỗi yêu cầu nạp lại User từ CSDL
 * một lần rồi nhớ trong request attribute, nên trạng thái khoá tài khoản có
 * hiệu lực ngay chứ không chờ người dùng đăng xuất.
 *
 * Chống dò mật khẩu theo mục 12: sai {@value #SO_LAN_TOI_DA} lần thì khoá
 * tạm {@value #PHUT_KHOA} phút.
 */
@Service
public class AuthService {

    public static final String USER_ID      = "authUserId";
    public static final String INTENDED_URL = "intendedUrl";
    private static final String REQUEST_CACHE = "__authUser";

    public static final int SO_LAN_TOI_DA = 5;
    public static final int PHUT_KHOA     = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ----------------------------------------------------------- phiên làm việc

    /** Người đang đăng nhập, hoặc null. Nạp kèm vai trò. */
    public User currentUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object daCo = request.getAttribute(REQUEST_CACHE);
        if (daCo instanceof User u) {
            return u;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(USER_ID);
        if (!(id instanceof Long userId)) {
            return null;
        }
        Optional<User> tim = userRepository.findWithRolesById(userId);
        if (tim.isEmpty() || !tim.get().isActive()) {
            // Tài khoản bị khoá hoặc đã xoá -> huỷ phiên ngay
            session.removeAttribute(USER_ID);
            return null;
        }
        User user = tim.get();
        request.setAttribute(REQUEST_CACHE, user);
        return user;
    }

    public Long currentUserId(HttpServletRequest request) {
        User u = currentUser(request);
        return u == null ? null : u.getId();
    }

    /** Người đang đăng nhập, ném lỗi nếu chưa — dùng trong service khi
     *  interceptor đã chắc chắn chặn khách vãng lai. */
    public User batBuocDangNhap(HttpServletRequest request) {
        User u = currentUser(request);
        if (u == null) {
            throw new LoiNghiepVu("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.");
        }
        return u;
    }

    // ---------------------------------------------------------------- đăng nhập

    /**
     * Kiểm tra email + mật khẩu và mở phiên.
     *
     * @throws LoiNghiepVu khi sai thông tin, tài khoản bị khoá, hoặc đang bị
     *                     khoá tạm vì nhập sai quá nhiều lần
     */
    @Transactional
    public User dangNhap(HttpServletRequest request, String email, String matKhau) {
        String e = email == null ? "" : email.trim();
        Optional<User> tim = userRepository.findByEmailIgnoreCase(e);

        // Không tiết lộ email có tồn tại hay không
        if (tim.isEmpty()) {
            throw new LoiNghiepVu("email", "Email hoặc mật khẩu không đúng.");
        }
        User user = tim.get();

        if (user.isTemporarilyLocked()) {
            long phut = Math.max(1, Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1);
            throw new LoiNghiepVu("Tài khoản đang tạm khoá do nhập sai nhiều lần. Vui lòng thử lại sau "
                    + phut + " phút.");
        }

        if (User.LOCKED.equals(user.getStatus())) {
            throw new LoiNghiepVu("Tài khoản đã bị khoá. Vui lòng liên hệ bộ phận hỗ trợ.");
        }

        if (!passwordEncoder.matches(matKhau == null ? "" : matKhau, user.getPasswordHash())) {
            ghiNhanSai(user);
            throw new LoiNghiepVu("email", "Email hoặc mật khẩu không đúng.");
        }

        if (User.PENDING_VERIFICATION.equals(user.getStatus())) {
            throw new LoiNghiepVu("Tài khoản chưa xác minh email. Vui lòng kiểm tra hộp thư.");
        }

        // Thành công: xoá bộ đếm, ghi lần đăng nhập
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        moPhien(request, user);
        return user;
    }

    private void ghiNhanSai(User user) {
        int lan = (user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount()) + 1;
        user.setFailedLoginCount(lan);
        if (lan >= SO_LAN_TOI_DA) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(PHUT_KHOA));
            user.setFailedLoginCount(0);
        }
        userRepository.save(user);
    }

    /** Mở phiên mới, đổi id phiên để chống session fixation. */
    public void moPhien(HttpServletRequest request, User user) {
        HttpSession cu = request.getSession(false);
        if (cu != null) {
            cu.invalidate();
        }
        HttpSession moi = request.getSession(true);
        moi.setAttribute(USER_ID, user.getId());
        request.setAttribute(REQUEST_CACHE, user);
    }

    public void dangXuat(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        request.removeAttribute(REQUEST_CACHE);
    }

    /** Lấy và xoá địa chỉ người dùng định vào trước khi bị bắt đăng nhập. */
    public String layDiaChiDinhVao(HttpServletRequest request, String macDinh) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return macDinh;
        }
        Object url = session.getAttribute(INTENDED_URL);
        session.removeAttribute(INTENDED_URL);
        return url instanceof String s && !s.isBlank() ? s : macDinh;
    }
}
