package com.smartwash.admin;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.brand.BrandService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Paginator;
import com.smartwash.store.StoreService;
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

/** Quản trị gian hàng (đặc tả mục 7.5, BR-06). */
@Controller
@RequestMapping("/admin/stores")
public class AdminStoreController {

    private final AuthService authService;
    private final StoreService storeService;
    private final BrandService brandService;

    public AdminStoreController(AuthService authService, StoreService storeService, BrandService brandService) {
        this.authService = authService;
        this.storeService = storeService;
        this.brandService = brandService;
    }

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long brandId,
                           @RequestParam(required = false) String type,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        model.addAttribute("ds", new Paginator<>(
                storeService.danhSach(q, status, brandId, type, page, 20), request));
        model.addAttribute("thuongHieu", brandService.choChonHoSo());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("brandId", brandId);
        model.addAttribute("type", type);
        return "admin/gian-hang";
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        model.addAttribute("store", storeService.timHoacBao(id));
        model.addAttribute("nhanSu", storeService.nhanSu(id));
        return "admin/chi-tiet-gian-hang";
    }

    @PostMapping("/{id}/suspend")
    public String tamNgung(HttpServletRequest request, @PathVariable Long id,
                           @RequestParam String reason, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            storeService.tamNgung(id, reason, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã tạm ngưng gian hàng.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/stores/" + id;
    }

    @PostMapping("/{id}/reactivate")
    public String moLai(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            storeService.moLai(id, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Gian hàng đã hoạt động trở lại.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/stores/" + id;
    }

    @PostMapping("/{id}/close")
    public String dong(HttpServletRequest request, @PathVariable Long id,
                       @RequestParam(required = false) String reason, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            storeService.dong(id, reason, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã đóng gian hàng.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/stores/" + id;
    }
}
