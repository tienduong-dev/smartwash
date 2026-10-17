package com.smartwash.admin;

import com.smartwash.brand.Brand;
import com.smartwash.brand.BrandRepository;
import com.smartwash.business.Business;
import com.smartwash.business.BusinessRepository;
import com.smartwash.sellerregistration.SellerRegistration;
import com.smartwash.sellerregistration.SellerRegistrationRepository;
import com.smartwash.sellerregistration.SellerRegistrationService;
import com.smartwash.store.Store;
import com.smartwash.store.StoreRepository;
import com.smartwash.user.User;
import com.smartwash.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Tổng quan khu quản trị (đặc tả mục 7.1). */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final BusinessRepository businessRepository;
    private final StoreRepository storeRepository;
    private final SellerRegistrationRepository registrationRepository;
    private final SellerRegistrationService registrationService;

    public AdminDashboardController(UserRepository userRepository,
                                    BrandRepository brandRepository,
                                    BusinessRepository businessRepository,
                                    StoreRepository storeRepository,
                                    SellerRegistrationRepository registrationRepository,
                                    SellerRegistrationService registrationService) {
        this.userRepository = userRepository;
        this.brandRepository = brandRepository;
        this.businessRepository = businessRepository;
        this.storeRepository = storeRepository;
        this.registrationRepository = registrationRepository;
        this.registrationService = registrationService;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String tongQuan(Model model) {
        model.addAttribute("soTaiKhoan", userRepository.count());
        model.addAttribute("soTaiKhoanMoi",
                userRepository.demTaoTu(LocalDate.now().minusDays(30).atStartOfDay()));
        model.addAttribute("soGianHang", storeRepository.countByStatus(Store.ACTIVE));
        model.addAttribute("soGianHangTamNgung", storeRepository.countByStatus(Store.SUSPENDED));
        model.addAttribute("soThuongHieu", brandRepository.countByStatus(Brand.ACTIVE));
        model.addAttribute("soThuongHieuChoDuyet", brandRepository.countByStatus(Brand.PENDING_VERIFICATION));
        model.addAttribute("soDoanhNghiepChoXacMinh",
                businessRepository.countByVerificationStatus(Business.PENDING));
        model.addAttribute("soHoSoChoXuLy", registrationService.soChoXuLy());
        model.addAttribute("soHoSoCanBoSung",
                registrationRepository.countByStatus(SellerRegistration.NEEDS_MORE_INFO));

        model.addAttribute("hoSoMoi", registrationRepository.timKiem(
                null, SellerRegistration.PENDING, null, PageRequest.of(0, 6)).getContent());
        return "admin/tong-quan";
    }
}
