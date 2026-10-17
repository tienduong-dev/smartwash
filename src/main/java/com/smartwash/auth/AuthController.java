package com.smartwash.auth;

import com.smartwash.common.FormErrors;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Messages;
import com.smartwash.common.OldInput;
import com.smartwash.common.Req;
import com.smartwash.user.User;
import com.smartwash.user.UserRepository;
import com.smartwash.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Đăng nhập, đăng ký, quên mật khẩu (đặc tả mục 6.1, 12). */
@Controller
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;

    public AuthController(AuthService authService,
                          UserService userService,
                          UserRepository userRepository,
                          PasswordResetTokenRepository tokenRepository) {
        this.authService = authService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    // ------------------------------------------------------------ đăng nhập

    @GetMapping("/login")
    public String formDangNhap(HttpServletRequest request) {
        return authService.currentUser(request) != null ? "redirect:/" : "auth/dang-nhap";
    }

    @PostMapping("/login")
    public String dangNhap(HttpServletRequest request,
                           @RequestParam String email,
                           @RequestParam String password,
                           Model model) {
        try {
            User u = authService.dangNhap(request, email, password);
            String dich = authService.layDiaChiDinhVao(request, u.isAdmin() ? "/admin" : "/");
            return "redirect:" + dich;
        } catch (LoiNghiepVu e) {
            return laiForm(model, request, "auth/dang-nhap", e);
        }
    }

    @PostMapping("/logout")
    public String dangXuat(HttpServletRequest request, RedirectAttributes ra) {
        authService.dangXuat(request);
        ra.addFlashAttribute("success", "Bạn đã đăng xuất.");
        return "redirect:/";
    }

    // ------------------------------------------------------------- đăng ký

    @GetMapping("/register")
    public String formDangKy(HttpServletRequest request) {
        return authService.currentUser(request) != null ? "redirect:/" : "auth/dang-ky";
    }

    @PostMapping("/register")
    public String dangKy(HttpServletRequest request,
                         @RequestParam String fullName,
                         @RequestParam String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam String password,
                         @RequestParam String passwordConfirm,
                         Model model,
                         RedirectAttributes ra) {
        try {
            FormErrors loi = new FormErrors();
            if (!Req.filled(fullName)) {
                loi.add("fullName", Messages.required("họ tên"));
            }
            if (!Req.isEmail(email)) {
                loi.add("email", Messages.email("email"));
            }
            if (Req.filled(phone) && !phone.trim().matches("^0\\d{9}$")) {
                loi.add("phone", Messages.phone("số điện thoại"));
            }
            if (password == null || password.length() < 8) {
                loi.add("password", Messages.min("mật khẩu", 8));
            } else if (!password.equals(passwordConfirm)) {
                loi.add("passwordConfirm", Messages.confirmed("mật khẩu"));
            }
            if (loi.any()) {
                model.addAttribute("errors", loi);
                model.addAttribute("old", new OldInput(request.getParameterMap()));
                return "auth/dang-ky";
            }

            User u = userService.dangKy(fullName, email, phone, password);
            authService.moPhien(request, u);
            ra.addFlashAttribute("success", "Chào mừng " + u.getFullName() + " đến với SmartWash.");
            return "redirect:/";
        } catch (LoiNghiepVu e) {
            return laiForm(model, request, "auth/dang-ky", e);
        }
    }

    // -------------------------------------------------------- quên mật khẩu

    @GetMapping("/forgot-password")
    public String formQuenMatKhau() {
        return "auth/quen-mat-khau";
    }

    /**
     * Gửi mã đặt lại.
     *
     * Luôn trả cùng một thông báo dù email có tồn tại hay không, để không
     * biến trang này thành công cụ dò xem ai đã đăng ký.
     */
    @PostMapping("/forgot-password")
    @Transactional
    public String guiMaDatLai(@RequestParam String email, RedirectAttributes ra) {
        Optional<User> tim = userRepository.findByEmailIgnoreCase(Req.trim(email));
        if (tim.isPresent()) {
            PasswordResetToken t = tokenRepository.findById(tim.get().getEmail())
                    .orElseGet(PasswordResetToken::new);
            t.setEmail(tim.get().getEmail());
            t.setToken(UUID.randomUUID().toString().replace("-", ""));
            t.setExpiresAt(LocalDateTime.now().plusHours(1));
            t.setCreatedAt(LocalDateTime.now());
            tokenRepository.save(t);
            // Gửi email nằm ở giai đoạn sau; hiện tại ghi ra log để thử nghiệm
            System.out.println("[Đặt lại mật khẩu] " + t.getEmail() + " -> /reset-password?token=" + t.getToken());
        }
        ra.addFlashAttribute("success",
                "Nếu email này đã đăng ký, chúng tôi vừa gửi hướng dẫn đặt lại mật khẩu.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String formDatLai(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "auth/dat-lai-mat-khau";
    }

    @PostMapping("/reset-password")
    @Transactional
    public String datLai(@RequestParam String token,
                         @RequestParam String password,
                         @RequestParam String passwordConfirm,
                         Model model,
                         RedirectAttributes ra) {
        PasswordResetToken t = tokenRepository.findByToken(token).orElse(null);
        if (t == null || t.isExpired()) {
            model.addAttribute("token", token);
            FormErrors loi = new FormErrors();
            loi.add("token", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            model.addAttribute("errors", loi);
            return "auth/dat-lai-mat-khau";
        }
        if (password == null || password.length() < 8 || !password.equals(passwordConfirm)) {
            model.addAttribute("token", token);
            FormErrors loi = new FormErrors();
            loi.add("password", password == null || password.length() < 8
                    ? Messages.min("mật khẩu", 8) : Messages.confirmed("mật khẩu"));
            model.addAttribute("errors", loi);
            return "auth/dat-lai-mat-khau";
        }

        User u = userRepository.findByEmailIgnoreCase(t.getEmail()).orElseThrow();
        userService.datLaiMatKhau(u, password);
        tokenRepository.delete(t);

        ra.addFlashAttribute("success", "Đã đổi mật khẩu. Mời bạn đăng nhập.");
        return "redirect:/login";
    }

    // --------------------------------------------------------------- nội bộ

    private String laiForm(Model model, HttpServletRequest request, String view, LoiNghiepVu e) {
        FormErrors loi = new FormErrors();
        loi.add(e.coTruong() ? e.getTruong() : "_", e.getMessage());
        model.addAttribute("errors", loi);
        model.addAttribute("old", new OldInput(request.getParameterMap()));
        return view;
    }
}
