package com.smartwash.cart;

import com.smartwash.auth.AuthService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Giỏ hàng của khách (đặc tả mục 5.7). */
@Controller
public class CartController {

    private final AuthService authService;
    private final CartService cartService;

    public CartController(AuthService authService, CartService cartService) {
        this.authService = authService;
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    public String gio(HttpServletRequest request, Model model) {
        User u = authService.batBuocDangNhap(request);
        Cart gio = cartService.gioDeHienThi(u.getId());
        model.addAttribute("gio", gio);
        model.addAttribute("nhom", cartService.nhomTheoGianHang(gio));
        return "gio-hang/index";
    }

    @PostMapping("/cart/{productId}")
    public String them(HttpServletRequest request,
                       @PathVariable Long productId,
                       @RequestParam(defaultValue = "1") int quantity,
                       @RequestParam(required = false) String next,
                       RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            cartService.them(u, productId, quantity);
            ra.addFlashAttribute("success", "Đã thêm vào giỏ hàng.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        // Chỉ nhận đường dẫn nội bộ: nhận cả URL ngoài thì thành lỗ hổng
        // chuyển hướng mở, kẻ xấu gửi link /cart/5?next=//trang-gia-mao.
        return "redirect:" + (next != null && next.startsWith("/") && !next.startsWith("//")
                ? next : "/cart");
    }

    @PostMapping("/cart/items/{itemId}")
    public String doiSoLuong(HttpServletRequest request,
                             @PathVariable Long itemId,
                             @RequestParam int quantity,
                             RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            cartService.doiSoLuong(u, itemId, quantity);
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{itemId}/remove")
    public String xoa(HttpServletRequest request,
                      @PathVariable Long itemId,
                      RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            cartService.xoa(u, itemId);
            ra.addFlashAttribute("success", "Đã xoá khỏi giỏ hàng.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String xoaHet(HttpServletRequest request, RedirectAttributes ra) {
        cartService.xoaHet(authService.batBuocDangNhap(request));
        ra.addFlashAttribute("success", "Đã xoá toàn bộ giỏ hàng.");
        return "redirect:/cart";
    }

    @PostMapping("/cart/clean")
    public String xoaDongVuong(HttpServletRequest request, RedirectAttributes ra) {
        int n = cartService.xoaDongVuong(authService.batBuocDangNhap(request));
        ra.addFlashAttribute("success", n == 0
                ? "Không có sản phẩm nào cần bỏ."
                : "Đã bỏ " + n + " sản phẩm không còn mua được.");
        return "redirect:/cart";
    }
}
