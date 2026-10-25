package com.smartwash.order;

import com.smartwash.auth.AuthService;
import com.smartwash.cart.Cart;
import com.smartwash.cart.CartService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.user.Address;
import com.smartwash.user.AddressRepository;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Trang đặt hàng (đặc tả mục 5.8). */
@Controller
public class CheckoutController {

    private final AuthService authService;
    private final CartService cartService;
    private final OrderService orderService;
    private final AddressRepository addressRepo;

    public CheckoutController(AuthService authService,
                              CartService cartService,
                              OrderService orderService,
                              AddressRepository addressRepo) {
        this.authService = authService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.addressRepo = addressRepo;
    }

    @GetMapping("/checkout")
    public String trang(HttpServletRequest request, Model model, RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        Cart gio = cartService.gioDeHienThi(u.getId());

        if (gio.isEmpty()) {
            ra.addFlashAttribute("error", "Giỏ hàng đang trống.");
            return "redirect:/cart";
        }
        // Giỏ còn dòng vướng thì chặn ngay ở đây cho khách tự xử lý, thay vì
        // để họ điền hết địa chỉ rồi mới báo lỗi ở bước cuối.
        if (gio.hasBlockedItem()) {
            ra.addFlashAttribute("error",
                    "Giỏ hàng còn sản phẩm không mua được. Hãy bỏ hoặc sửa số lượng trước khi đặt.");
            return "redirect:/cart";
        }

        List<Address> diaChi = addressRepo.findByUserIdOrderByIsDefaultDescIdDesc(u.getId());
        model.addAttribute("gio", gio);
        model.addAttribute("nhom", cartService.nhomTheoGianHang(gio));
        model.addAttribute("diaChi", diaChi);
        model.addAttribute("diaChiChon", diaChi.isEmpty() ? null : diaChi.get(0).getId());
        return "dat-hang/index";
    }

    @PostMapping("/checkout")
    public String datHang(HttpServletRequest request,
                          @RequestParam(required = false) Long addressId,
                          @RequestParam(defaultValue = "COD") String paymentMethod,
                          @RequestParam(required = false) String note,
                          RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            if (addressId == null) {
                throw new LoiNghiepVu("Hãy chọn địa chỉ nhận hàng.");
            }
            // Tra địa chỉ kèm user_id, không tra theo mình id: đổi số trên form
            // thì không được phép lấy địa chỉ của người khác.
            Address dc = addressRepo.findByIdAndUserId(addressId, u.getId())
                    .orElseThrow(() -> new LoiNghiepVu("Địa chỉ không hợp lệ."));

            Order don = orderService.datHang(u, dc, paymentMethod, note);
            ra.addFlashAttribute("success",
                    "Đặt hàng thành công. Mã đơn của bạn là " + don.getOrderCode() + ".");
            return "redirect:/orders/" + don.getOrderCode();

        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }
}
