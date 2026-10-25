package com.smartwash.order;

import com.smartwash.auth.AuthService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Paginator;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Đơn hàng của khách (đặc tả mục 5.8). */
@Controller
public class OrderController {

    private final AuthService authService;
    private final OrderService orderService;

    public OrderController(AuthService authService, OrderService orderService) {
        this.authService = authService;
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String danhSach(HttpServletRequest request,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        User u = authService.batBuocDangNhap(request);
        model.addAttribute("ds", new Paginator<>(orderService.cuaKhach(u.getId(), page, 10), request));
        return "don-hang/danh-sach";
    }

    @GetMapping("/orders/{maDon}")
    public String chiTiet(HttpServletRequest request, @PathVariable String maDon, Model model) {
        User u = authService.batBuocDangNhap(request);
        Order don = orderService.chiTiet(u, maDon);

        // Lịch sử trạng thái của từng đơn con, tra sẵn một lượt để template
        // không phải gọi service trong vòng lặp.
        Map<Long, List<OrderStatusHistory>> lichSu = new LinkedHashMap<>();
        for (StoreOrder so : don.getStoreOrders()) {
            lichSu.put(so.getId(), orderService.lichSu(so.getId()));
        }

        model.addAttribute("don", don);
        model.addAttribute("lichSu", lichSu);
        return "don-hang/chi-tiet";
    }

    @PostMapping("/orders/{maDon}/cancel")
    public String huy(HttpServletRequest request,
                      @PathVariable String maDon,
                      @RequestParam(required = false) String reason,
                      RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            orderService.khachHuyDon(u, maDon, reason);
            ra.addFlashAttribute("success", "Đã huỷ đơn hàng " + maDon + ".");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + maDon;
    }
}
