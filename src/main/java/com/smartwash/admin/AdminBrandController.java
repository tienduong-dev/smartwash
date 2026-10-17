package com.smartwash.admin;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.brand.Brand;
import com.smartwash.brand.BrandService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.LuuTru;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Quản trị thương hiệu (đặc tả mục 7.3, BR-02). */
@Controller
@RequestMapping("/admin/brands")
public class AdminBrandController {

    private final AuthService authService;
    private final BrandService brandService;
    private final StoreService storeService;
    private final LuuTru luuTru;

    public AdminBrandController(AuthService authService, BrandService brandService,
                                StoreService storeService, LuuTru luuTru) {
        this.authService = authService;
        this.brandService = brandService;
        this.storeService = storeService;
        this.luuTru = luuTru;
    }

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        model.addAttribute("ds", new Paginator<>(brandService.danhSach(q, status, page, 24), request));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "admin/thuong-hieu";
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        Brand b = brandService.timHoacBao(id);
        model.addAttribute("b", b);
        model.addAttribute("nghiTrung", brandService.nghiTrung(b.getName(), b.getId()));
        model.addAttribute("gianHang", storeService.danhSach(null, null, id, null, 1, 50).getContent());
        return "admin/chi-tiet-thuong-hieu";
    }

    @PostMapping
    public String tao(HttpServletRequest request,
                      @RequestParam String name,
                      @RequestParam(required = false) String description,
                      @RequestParam(required = false) String website,
                      @RequestParam(required = false) String country,
                      RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            Brand b = brandService.tao(name, description, website, country, admin, true,
                    AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã tạo thương hiệu " + b.getName() + ".");
            return "redirect:/admin/brands/" + b.getId();
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/brands";
        }
    }

    @PostMapping("/{id}")
    public String capNhat(HttpServletRequest request, @PathVariable Long id,
                          @RequestParam String name,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) String website,
                          @RequestParam(required = false) String country,
                          @RequestParam(required = false) MultipartFile logo,
                          RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            String duongDan = logo != null && !logo.isEmpty() ? luuTru.luuAnh(logo, "brands") : null;
            brandService.capNhat(id, name, description, website, country, duongDan, admin,
                    AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã lưu thương hiệu.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/brands/" + id;
    }

    @PostMapping("/{id}/verify")
    public String xacMinh(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        brandService.xacMinh(id, admin, AuditService.ipCua(request));
        ra.addFlashAttribute("success", "Đã xác minh thương hiệu.");
        return "redirect:/admin/brands/" + id;
    }

    @PostMapping("/{id}/lock")
    public String khoa(HttpServletRequest request, @PathVariable Long id,
                       @RequestParam(required = false) String reason, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        brandService.khoa(id, reason, admin, AuditService.ipCua(request));
        ra.addFlashAttribute("success", "Đã khoá thương hiệu. Các gian hàng gắn thương hiệu này tạm thời không bán được.");
        return "redirect:/admin/brands/" + id;
    }

    @PostMapping("/{id}/unlock")
    public String moKhoa(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        brandService.moKhoa(id, admin, AuditService.ipCua(request));
        ra.addFlashAttribute("success", "Đã mở khoá thương hiệu.");
        return "redirect:/admin/brands/" + id;
    }
}
