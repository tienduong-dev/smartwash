package com.smartwash.store;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.LuuTru;
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

/** Khu người bán: tổng quan, hồ sơ gian hàng, nhân sự (đặc tả mục 6.3, 6.10). */
@Controller
@RequestMapping("/seller/{storeId}")
public class SellerStoreController {

    private final AuthService authService;
    private final StoreContext storeContext;
    private final StoreService storeService;
    private final LuuTru luuTru;

    public SellerStoreController(AuthService authService,
                                 StoreContext storeContext,
                                 StoreService storeService,
                                 LuuTru luuTru) {
        this.authService = authService;
        this.storeContext = storeContext;
        this.storeService = storeService;
        this.luuTru = luuTru;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String tongQuan(HttpServletRequest request, @PathVariable Long storeId, Model model) {
        User u = authService.batBuocDangNhap(request);
        Store s = storeContext.batBuocGianHang(storeId, u);
        model.addAttribute("store", s);
        model.addAttribute("nhanSu", storeService.nhanSu(storeId));
        // Các con số bán hàng sẽ nối vào ở giai đoạn đơn hàng
        return "nguoi-ban/tong-quan";
    }

    // ------------------------------------------------------- hồ sơ gian hàng

    @GetMapping("/profile")
    public String hoSo(HttpServletRequest request, @PathVariable Long storeId, Model model) {
        User u = authService.batBuocDangNhap(request);
        model.addAttribute("store", storeContext.batBuocGianHang(storeId, u));
        return "nguoi-ban/thong-tin";
    }

    @PostMapping("/profile")
    public String luuHoSo(HttpServletRequest request,
                          @PathVariable Long storeId,
                          @RequestParam String name,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) String phone,
                          @RequestParam(required = false) String email,
                          @RequestParam(required = false) String pickupAddress,
                          @RequestParam(required = false) String returnPolicy,
                          @RequestParam(required = false) String warrantyPolicy,
                          @RequestParam(required = false) String shippingPolicy,
                          @RequestParam(required = false) MultipartFile logo,
                          @RequestParam(required = false) MultipartFile banner,
                          RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        StoreMember tc = storeContext.batBuocThanhVien(storeId, u);
        if (!tc.isOwner()) {
            throw new LoiNghiepVu("Chỉ chủ gian hàng mới sửa được thông tin gian hàng.");
        }
        try {
            String duongDanLogo = logo != null && !logo.isEmpty() ? luuTru.luuAnh(logo, "stores") : null;
            String duongDanBanner = banner != null && !banner.isEmpty() ? luuTru.luuAnh(banner, "stores") : null;
            storeService.capNhatHoSo(storeId, name, description, phone, email, pickupAddress,
                    returnPolicy, warrantyPolicy, shippingPolicy, duongDanLogo, duongDanBanner);
            ra.addFlashAttribute("success", "Đã lưu thông tin gian hàng.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/profile";
    }

    // ------------------------------------------------------------- nhân sự

    @GetMapping("/members")
    public String nhanSu(HttpServletRequest request, @PathVariable Long storeId, Model model) {
        User u = authService.batBuocDangNhap(request);
        StoreMember tc = storeContext.batBuocThanhVien(storeId, u);
        if (!tc.isOwner()) {
            throw new LoiNghiepVu("Chỉ chủ gian hàng mới quản lý được nhân sự.");
        }
        model.addAttribute("store", storeContext.batBuocGianHang(storeId, u));
        model.addAttribute("nhanSu", storeService.nhanSu(storeId));
        return "nguoi-ban/nhan-su";
    }

    @PostMapping("/members")
    public String themNhanVien(HttpServletRequest request,
                               @PathVariable Long storeId,
                               @RequestParam String email,
                               @RequestParam(defaultValue = "false") boolean canManageProducts,
                               @RequestParam(defaultValue = "false") boolean canHandleOrders,
                               @RequestParam(defaultValue = "false") boolean canSupportCustomers,
                               @RequestParam(defaultValue = "false") boolean canViewReports,
                               RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        StoreMember tc = storeContext.batBuocThanhVien(storeId, u);
        if (!tc.isOwner()) {
            throw new LoiNghiepVu("Chỉ chủ gian hàng mới thêm được nhân sự.");
        }
        try {
            storeService.themNhanVien(storeId, email, canManageProducts, canHandleOrders,
                    canSupportCustomers, canViewReports, u, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã thêm nhân viên vào gian hàng.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/members";
    }

    @PostMapping("/members/{memberId}/permissions")
    public String doiQuyen(HttpServletRequest request,
                           @PathVariable Long storeId,
                           @PathVariable Long memberId,
                           @RequestParam(defaultValue = "false") boolean canManageProducts,
                           @RequestParam(defaultValue = "false") boolean canHandleOrders,
                           @RequestParam(defaultValue = "false") boolean canSupportCustomers,
                           @RequestParam(defaultValue = "false") boolean canViewReports,
                           RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        StoreMember tc = storeContext.batBuocThanhVien(storeId, u);
        if (!tc.isOwner()) {
            throw new LoiNghiepVu("Chỉ chủ gian hàng mới đổi được quyền.");
        }
        try {
            storeService.doiQuyen(memberId, canManageProducts, canHandleOrders,
                    canSupportCustomers, canViewReports, u, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã cập nhật quyền.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/members";
    }

    @PostMapping("/members/{memberId}/revoke")
    public String thuHoi(HttpServletRequest request,
                         @PathVariable Long storeId,
                         @PathVariable Long memberId,
                         RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        StoreMember tc = storeContext.batBuocThanhVien(storeId, u);
        if (!tc.isOwner()) {
            throw new LoiNghiepVu("Chỉ chủ gian hàng mới thu hồi được quyền.");
        }
        try {
            storeService.thuHoi(memberId, u, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã thu hồi quyền truy cập.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/members";
    }
}
