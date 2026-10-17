package com.smartwash.user;

import com.smartwash.auth.AuthService;
import com.smartwash.common.FormErrors;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.LuuTru;
import com.smartwash.common.Messages;
import com.smartwash.common.Req;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Hồ sơ cá nhân và sổ địa chỉ (đặc tả mục 5.6, 6.2). */
@Controller
public class AccountController {

    private final AuthService authService;
    private final UserService userService;
    private final AddressRepository addressRepository;
    private final LuuTru luuTru;

    public AccountController(AuthService authService,
                             UserService userService,
                             AddressRepository addressRepository,
                             LuuTru luuTru) {
        this.authService = authService;
        this.userService = userService;
        this.addressRepository = addressRepository;
        this.luuTru = luuTru;
    }

    // --------------------------------------------------------------- hồ sơ

    @GetMapping("/account")
    public String hoSo(HttpServletRequest request, Model model) {
        model.addAttribute("user", authService.batBuocDangNhap(request));
        return "tai-khoan/ho-so";
    }

    @PostMapping("/account")
    public String luuHoSo(HttpServletRequest request,
                          @RequestParam String fullName,
                          @RequestParam(required = false) String phone,
                          @RequestParam(required = false) MultipartFile avatar,
                          RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            String anh = avatar != null && !avatar.isEmpty() ? luuTru.luuAnh(avatar, "avatars") : null;
            userService.capNhatHoSo(u, fullName, phone, anh);
            ra.addFlashAttribute("success", "Đã cập nhật hồ sơ.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/account";
    }

    @GetMapping("/account/password")
    public String formDoiMatKhau() {
        return "tai-khoan/doi-mat-khau";
    }

    @PostMapping("/account/password")
    public String doiMatKhau(HttpServletRequest request,
                             @RequestParam String currentPassword,
                             @RequestParam String newPassword,
                             @RequestParam String newPasswordConfirm,
                             Model model,
                             RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            if (!newPassword.equals(newPasswordConfirm)) {
                throw new LoiNghiepVu("newPasswordConfirm", Messages.confirmed("mật khẩu mới"));
            }
            userService.doiMatKhau(u, currentPassword, newPassword);
            ra.addFlashAttribute("success", "Đã đổi mật khẩu.");
            return "redirect:/account";
        } catch (LoiNghiepVu e) {
            FormErrors loi = new FormErrors();
            loi.add(e.coTruong() ? e.getTruong() : "_", e.getMessage());
            model.addAttribute("errors", loi);
            return "tai-khoan/doi-mat-khau";
        }
    }

    // ------------------------------------------------------------- địa chỉ

    @GetMapping("/addresses")
    public String danhSachDiaChi(HttpServletRequest request, Model model) {
        User u = authService.batBuocDangNhap(request);
        model.addAttribute("diaChi", addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(u.getId()));
        return "tai-khoan/dia-chi";
    }

    @PostMapping("/addresses")
    @Transactional
    public String themDiaChi(HttpServletRequest request,
                             @RequestParam(required = false) String label,
                             @RequestParam String receiverName,
                             @RequestParam String phone,
                             @RequestParam String line,
                             @RequestParam Integer provinceId,
                             @RequestParam String provinceName,
                             @RequestParam Integer districtId,
                             @RequestParam String districtName,
                             @RequestParam String wardCode,
                             @RequestParam String wardName,
                             @RequestParam(defaultValue = "false") boolean isDefault,
                             RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        if (!Req.filled(receiverName) || !Req.filled(line)) {
            ra.addFlashAttribute("error", "Vui lòng nhập đủ tên người nhận và địa chỉ.");
            return "redirect:/addresses";
        }
        if (!phone.trim().matches("^0\\d{9}$")) {
            ra.addFlashAttribute("error", Messages.phone("số điện thoại"));
            return "redirect:/addresses";
        }

        Address a = new Address();
        a.setUser(u);
        a.setLabel(Req.nullIfBlank(label));
        a.setReceiverName(receiverName.trim());
        a.setPhone(phone.trim());
        a.setLine(line.trim());
        a.setProvinceId(provinceId);
        a.setProvinceName(provinceName);
        a.setDistrictId(districtId);
        a.setDistrictName(districtName);
        a.setWardCode(wardCode);
        a.setWardName(wardName);
        // Địa chỉ đầu tiên luôn là mặc định, khỏi bắt người dùng tự tích
        a.setIsDefault(isDefault || addressRepository.countByUserId(u.getId()) == 0);
        addressRepository.save(a);

        if (Boolean.TRUE.equals(a.getIsDefault())) {
            addressRepository.boMacDinhKhac(u.getId(), a.getId());
        }
        ra.addFlashAttribute("success", "Đã thêm địa chỉ.");
        return "redirect:/addresses";
    }

    @PostMapping("/addresses/{id}/default")
    @Transactional
    public String datMacDinh(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        Address a = addressRepository.findByIdAndUserId(id, u.getId())
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy địa chỉ."));
        a.setIsDefault(true);
        addressRepository.save(a);
        addressRepository.boMacDinhKhac(u.getId(), a.getId());
        ra.addFlashAttribute("success", "Đã đặt làm địa chỉ mặc định.");
        return "redirect:/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    @Transactional
    public String xoaDiaChi(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        Address a = addressRepository.findByIdAndUserId(id, u.getId())
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy địa chỉ."));
        boolean laMacDinh = a.isDefaultAddress();
        addressRepository.delete(a);

        // Xoá địa chỉ mặc định thì nâng địa chỉ còn lại lên làm mặc định
        if (laMacDinh) {
            addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(u.getId()).stream()
                    .findFirst()
                    .ifPresent(con -> {
                        con.setIsDefault(true);
                        addressRepository.save(con);
                    });
        }
        ra.addFlashAttribute("success", "Đã xoá địa chỉ.");
        return "redirect:/addresses";
    }
}
