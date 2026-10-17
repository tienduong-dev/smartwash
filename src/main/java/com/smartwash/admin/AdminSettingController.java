package com.smartwash.admin;

import com.smartwash.audit.AuditLog;
import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.setting.Setting;
import com.smartwash.setting.SettingRepository;
import com.smartwash.setting.SettingService;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/** Cấu hình sàn (đặc tả mục 7.12). */
@Controller
@RequestMapping("/admin/settings")
public class AdminSettingController {

    private final AuthService authService;
    private final SettingService settingService;
    private final SettingRepository settingRepository;
    private final AuditService auditService;

    public AdminSettingController(AuthService authService, SettingService settingService,
                                  SettingRepository settingRepository, AuditService auditService) {
        this.authService = authService;
        this.settingService = settingService;
        this.settingRepository = settingRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public String trang(Model model) {
        model.addAttribute("cauHinh", settingRepository.findAll());
        model.addAttribute("tyLeHoaHong", settingService.tyLeHoaHong());
        model.addAttribute("soNgayDoiTra", settingService.soNgayDoiTra());
        model.addAttribute("batBuocDuyetSanPham", settingService.batBuocDuyetSanPham());
        return "admin/cau-hinh";
    }

    @PostMapping
    @Transactional
    public String luu(HttpServletRequest request,
                      @RequestParam String commissionRate,
                      @RequestParam String returnWindowDays,
                      @RequestParam(defaultValue = "false") boolean productReviewRequired,
                      RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);

        String hoaHongCu = settingService.tyLeHoaHong().toPlainString();
        String doiTraCu = String.valueOf(settingService.soNgayDoiTra());

        settingService.dat(Setting.COMMISSION_RATE, commissionRate.trim(), "Tỷ lệ phí sàn mặc định (%)");
        settingService.dat(Setting.RETURN_WINDOW_DAYS, returnWindowDays.trim(), "Số ngày được đổi trả");
        settingService.dat(Setting.PRODUCT_REVIEW_REQUIRED, productReviewRequired ? "1" : "0",
                "Bắt buộc Admin duyệt sản phẩm mới");

        auditService.ghi(admin, AuditLog.SETTING_CHANGED, "Setting", null,
                Map.of("commission_rate", hoaHongCu, "return_window_days", doiTraCu),
                Map.of("commission_rate", commissionRate.trim(),
                       "return_window_days", returnWindowDays.trim(),
                       "product_review_required", productReviewRequired ? "1" : "0"),
                null, AuditService.ipCua(request));

        ra.addFlashAttribute("success", "Đã lưu cấu hình.");
        return "redirect:/admin/settings";
    }
}
