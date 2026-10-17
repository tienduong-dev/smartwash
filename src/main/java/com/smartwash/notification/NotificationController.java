package com.smartwash.notification;

import com.smartwash.auth.AuthService;
import com.smartwash.common.Paginator;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Trung tâm thông báo (đặc tả mục 9.1). */
@Controller
public class NotificationController {

    private final AuthService authService;
    private final NotificationService notificationService;

    public NotificationController(AuthService authService, NotificationService notificationService) {
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String danhSach(HttpServletRequest request,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        User u = authService.batBuocDangNhap(request);
        model.addAttribute("ds", new Paginator<>(notificationService.danhSach(u.getId(), page, 20), request));
        return "tai-khoan/thong-bao";
    }

    @PostMapping("/notifications/{id}/read")
    public String danhDauDoc(HttpServletRequest request, @PathVariable Long id,
                             @RequestParam(required = false) String next) {
        User u = authService.batBuocDangNhap(request);
        notificationService.danhDauDoc(id, u.getId());
        return "redirect:" + (next == null || next.isBlank() || !next.startsWith("/") ? "/notifications" : next);
    }

    @PostMapping("/notifications/read-all")
    public String docHet(HttpServletRequest request, RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        int n = notificationService.danhDauDocHet(u.getId());
        ra.addFlashAttribute("success", "Đã đánh dấu " + n + " thông báo là đã đọc.");
        return "redirect:/notifications";
    }
}
