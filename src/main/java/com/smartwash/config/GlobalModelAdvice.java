package com.smartwash.config;

import com.smartwash.auth.AuthService;
import com.smartwash.cart.CartService;
import com.smartwash.common.FormErrors;
import com.smartwash.common.OldInput;
import com.smartwash.notification.NotificationService;
import com.smartwash.product.ProductService;
import com.smartwash.sellerregistration.SellerRegistrationService;
import com.smartwash.store.Store;
import com.smartwash.store.StoreContext;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Biến dùng chung cho mọi view: người đăng nhập, thông báo, gian hàng. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final AuthService authService;
    private final NotificationService notificationService;
    private final StoreContext storeContext;
    private final SellerRegistrationService registrationService;
    private final ProductService productService;
    private final CartService cartService;

    public GlobalModelAdvice(AuthService authService,
                             NotificationService notificationService,
                             StoreContext storeContext,
                             SellerRegistrationService registrationService,
                             ProductService productService,
                             CartService cartService) {
        this.authService = authService;
        this.notificationService = notificationService;
        this.storeContext = storeContext;
        this.registrationService = registrationService;
        this.productService = productService;
        this.cartService = cartService;
    }

    @ModelAttribute
    public void bienChung(HttpServletRequest request, Model model) {
        User user = authService.currentUser(request);
        String path = request.getRequestURI().substring(request.getContextPath().length());

        model.addAttribute("authUser", user);
        model.addAttribute("isLoggedIn", user != null);
        model.addAttribute("isAdmin", user != null && user.isAdmin());
        model.addAttribute("currentPath", path);

        if (!model.containsAttribute("errors")) {
            model.addAttribute("errors", new FormErrors());
        }
        if (!model.containsAttribute("old")) {
            model.addAttribute("old", new OldInput());
        }

        if (user == null) {
            return;
        }

        model.addAttribute("soThongBaoChuaDoc", notificationService.soChuaDoc(user.getId()));
        model.addAttribute("soMonTrongGio", cartService.soLuongTrenHeader(user.getId()));

        // Danh sách gian hàng để đổi nhanh ở thanh bên khu người bán
        if (path.startsWith("/seller")) {
            List<Store> cuaToi = storeContext.gianHangCua(user);
            model.addAttribute("gianHangCuaToi", cuaToi);
            Long storeId = StoreContext.idTuDuongDan(request);
            if (storeId != null) {
                cuaToi.stream()
                        .filter(s -> s.getId().equals(storeId))
                        .findFirst()
                        .ifPresent(s -> model.addAttribute("gianHangHienTai", s));
            }
            model.addAttribute("storeMember", request.getAttribute(AccessInterceptor.STORE_MEMBER));
        }

        // Con số trên menu quản trị
        if (user.isAdmin() && path.startsWith("/admin")) {
            model.addAttribute("navHoSoChoDuyet", registrationService.soChoXuLy());
            model.addAttribute("navSanPhamChoDuyet", productService.soChoDuyet());
        }
    }
}
