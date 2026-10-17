package com.smartwash.admin;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.brand.BrandService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Paginator;
import com.smartwash.sellerregistration.SellerRegistration;
import com.smartwash.sellerregistration.SellerRegistrationService;
import com.smartwash.store.Store;
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

/** Thẩm định hồ sơ mở gian hàng (đặc tả mục 7.4, BR-05). */
@Controller
@RequestMapping("/admin/registrations")
public class AdminRegistrationController {

    private final AuthService authService;
    private final SellerRegistrationService registrationService;
    private final BrandService brandService;

    public AdminRegistrationController(AuthService authService,
                                       SellerRegistrationService registrationService,
                                       BrandService brandService) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.brandService = brandService;
    }

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String type,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        model.addAttribute("ds", new Paginator<>(
                registrationService.danhSach(q, status, type, page, 20), request));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        return "admin/ho-so";
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        SellerRegistration r = registrationService.chiTiet(id);
        model.addAttribute("hoSo", r);
        // BR-02: cảnh báo nếu tên thương hiệu đề xuất trùng với brand đã có
        if (r.isNewBrandProposal()) {
            model.addAttribute("nghiTrung", brandService.nghiTrung(r.getRequestedBrandName(), null));
        }
        model.addAttribute("thuongHieu", brandService.choChonHoSo());
        return "admin/chi-tiet-ho-so";
    }

    @PostMapping("/{id}/review")
    public String tiepNhan(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            registrationService.tiepNhan(id, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã tiếp nhận hồ sơ để thẩm định.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/registrations/" + id;
    }

    @PostMapping("/{id}/need-info")
    public String yeuCauBoSung(HttpServletRequest request, @PathVariable Long id,
                               @RequestParam String note, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            registrationService.yeuCauBoSung(id, note, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã gửi yêu cầu bổ sung cho người nộp.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/registrations/" + id;
    }

    @PostMapping("/{id}/reject")
    public String tuChoi(HttpServletRequest request, @PathVariable Long id,
                         @RequestParam String note, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            registrationService.tuChoi(id, note, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã từ chối hồ sơ.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/registrations/" + id;
    }

    /** Duyệt: tạo gian hàng, gắn thương hiệu, cấp quyền — trong một giao dịch. */
    @PostMapping("/{id}/approve")
    public String duyet(HttpServletRequest request, @PathVariable Long id,
                        @RequestParam(required = false) Long brandId,
                        @RequestParam(required = false) String note,
                        RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            Store s = registrationService.duyet(id, brandId, note, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success",
                    "Đã duyệt hồ sơ. Gian hàng \"" + s.getName() + "\" đã mở và chủ gian hàng được cấp quyền.");
            return "redirect:/admin/stores/" + s.getId();
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/registrations/" + id;
        }
    }

    @PostMapping("/documents/{docId}")
    public String danhGiaGiayTo(@PathVariable Long docId,
                                @RequestParam Long registrationId,
                                @RequestParam String status,
                                @RequestParam(required = false) String note,
                                RedirectAttributes ra) {
        try {
            registrationService.danhGiaGiayTo(docId, status, note);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái giấy tờ.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/registrations/" + registrationId;
    }
}
