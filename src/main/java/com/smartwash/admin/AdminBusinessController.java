package com.smartwash.admin;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.business.BusinessService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Paginator;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Quản trị doanh nghiệp (đặc tả mục 7.3). */
@Controller
@RequestMapping("/admin/businesses")
public class AdminBusinessController {

    private final AuthService authService;
    private final BusinessService businessService;

    public AdminBusinessController(AuthService authService, BusinessService businessService) {
        this.authService = authService;
        this.businessService = businessService;
    }

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        model.addAttribute("ds", new Paginator<>(businessService.danhSach(q, status, page, 20), request));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "admin/doanh-nghiep";
    }

    @PostMapping("/{id}/verify")
    public String xacMinh(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        businessService.xacMinh(id, admin, AuditService.ipCua(request));
        ra.addFlashAttribute("success", "Đã xác minh doanh nghiệp.");
        return "redirect:/admin/businesses";
    }

    @PostMapping("/{id}/reject")
    public String tuChoi(HttpServletRequest request, @PathVariable Long id,
                         @RequestParam(required = false) String reason, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            businessService.tuChoi(id, reason, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã từ chối hồ sơ doanh nghiệp.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/businesses";
    }
}
