package com.smartwash.admin;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Paginator;
import com.smartwash.user.Role;
import com.smartwash.user.User;
import com.smartwash.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Quản trị tài khoản (đặc tả mục 7.2). */
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AuthService authService;
    private final UserService userService;

    public AdminUserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String role,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        model.addAttribute("ds", new Paginator<>(userService.danhSach(q, status, role, page, 20), request));
        model.addAttribute("vaiTro", userService.cacVaiTro());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("role", role);
        return "admin/tai-khoan";
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        model.addAttribute("u", userService.timHoacBao(id));
        model.addAttribute("vaiTro", userService.cacVaiTro());
        return "admin/chi-tiet-tai-khoan";
    }

    @PostMapping("/{id}/lock")
    public String khoa(HttpServletRequest request, @PathVariable Long id,
                       @RequestParam(required = false) String reason, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            userService.khoa(id, reason, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã khoá tài khoản.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/unlock")
    public String moKhoa(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            userService.moKhoa(id, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã mở khoá tài khoản.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/roles")
    public String capVaiTro(HttpServletRequest request, @PathVariable Long id,
                            @RequestParam String code, @RequestParam String action,
                            RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            User u = userService.timHoacBao(id);
            if ("remove".equals(action)) {
                if (Role.ADMIN.equals(code) && admin.getId().equals(id)) {
                    throw new LoiNghiepVu("Không thể tự bỏ quyền quản trị của chính mình.");
                }
                userService.boVaiTro(u, code, admin, AuditService.ipCua(request));
                ra.addFlashAttribute("success", "Đã bỏ vai trò.");
            } else {
                userService.themVaiTro(u, code, admin);
                ra.addFlashAttribute("success", "Đã cấp vai trò.");
            }
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
}
