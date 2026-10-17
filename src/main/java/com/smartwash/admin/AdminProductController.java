package com.smartwash.admin;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Paginator;
import com.smartwash.product.Product;
import com.smartwash.product.ProductService;
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

/** Kiểm duyệt sản phẩm toàn sàn (đặc tả mục 7.6). */
@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final AuthService authService;
    private final ProductService productService;
    private final StoreService storeService;

    public AdminProductController(AuthService authService,
                                  ProductService productService,
                                  StoreService storeService) {
        this.authService = authService;
        this.productService = productService;
        this.storeService = storeService;
    }

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long storeId,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        model.addAttribute("ds", new Paginator<>(
                productService.hangDoiDuyet(q, status, storeId, page, 20), request));
        model.addAttribute("gianHang", storeService.danhSach(null, "ACTIVE", null, null, 1, 100).getContent());
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("storeId", storeId);
        return "admin/duyet-san-pham";
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        Product p = productService.timHoacBao(id);
        model.addAttribute("p", p);
        return "admin/chi-tiet-san-pham";
    }

    @PostMapping("/{id}/approve")
    public String duyet(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            productService.duyet(id, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã duyệt sản phẩm.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/reject")
    public String tuChoi(HttpServletRequest request, @PathVariable Long id,
                         @RequestParam String reason, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            productService.tuChoi(id, reason, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã từ chối sản phẩm và gửi lý do cho người bán.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/suspend")
    public String go(HttpServletRequest request, @PathVariable Long id,
                     @RequestParam(required = false) String reason, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            productService.go(id, reason, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã gỡ sản phẩm khỏi sàn.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }

    @PostMapping("/{id}/restore")
    public String phucHoi(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User admin = authService.batBuocDangNhap(request);
        try {
            productService.phucHoi(id, admin, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã cho sản phẩm bán lại.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }
}
