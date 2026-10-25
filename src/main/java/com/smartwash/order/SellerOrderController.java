package com.smartwash.order;

import com.smartwash.auth.AuthService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Paginator;
import com.smartwash.store.Store;
import com.smartwash.store.StoreContext;
import com.smartwash.store.StoreMember;
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

/**
 * Khu người bán: xử lý đơn của một gian hàng (đặc tả mục 6.7).
 *
 * Interceptor đã chặn ở tầng ngoài: phải là thành viên đang làm việc của gian
 * hàng trong URL. Ở đây kiểm tra thêm quyền chi tiết — nhân viên có thể được
 * giao quản lý sản phẩm mà không được đụng vào đơn hàng.
 */
@Controller
@RequestMapping("/seller/{storeId}/orders")
public class SellerOrderController {

    private final AuthService authService;
    private final StoreContext storeContext;
    private final SellerOrderService sellerOrderService;

    public SellerOrderController(AuthService authService,
                                 StoreContext storeContext,
                                 SellerOrderService sellerOrderService) {
        this.authService = authService;
        this.storeContext = storeContext;
        this.sellerOrderService = sellerOrderService;
    }

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @PathVariable Long storeId,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        User u = authService.batBuocDangNhap(request);
        Store s = storeContext.batBuocGianHang(storeId, u);
        batBuocQuyenDonHang(storeId, u);

        model.addAttribute("store", s);
        model.addAttribute("ds", new Paginator<>(
                sellerOrderService.danhSach(storeId, status, q, page, 20), request));
        model.addAttribute("soChoXuLy", sellerOrderService.soChoXuLy(storeId));
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        return "nguoi-ban/don-hang";
    }

    @GetMapping("/{maDonCon}")
    public String chiTiet(HttpServletRequest request,
                          @PathVariable Long storeId,
                          @PathVariable String maDonCon,
                          Model model) {
        User u = authService.batBuocDangNhap(request);
        Store s = storeContext.batBuocGianHang(storeId, u);
        batBuocQuyenDonHang(storeId, u);

        StoreOrder so = sellerOrderService.chiTiet(storeId, maDonCon);
        model.addAttribute("store", s);
        model.addAttribute("so", so);
        model.addAttribute("lichSu", sellerOrderService.lichSu(so.getId()));
        model.addAttribute("buocTiep", sellerOrderService.buocTiepTheo(so));
        return "nguoi-ban/chi-tiet-don";
    }

    @PostMapping("/{maDonCon}/status")
    public String doiTrangThai(HttpServletRequest request,
                               @PathVariable Long storeId,
                               @PathVariable String maDonCon,
                               @RequestParam String to,
                               @RequestParam(required = false) String note,
                               RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        storeContext.batBuocGianHang(storeId, u);
        batBuocQuyenDonHang(storeId, u);

        try {
            StoreOrder so = sellerOrderService.doiTrangThai(storeId, maDonCon, to, u, note);
            ra.addFlashAttribute("success", "Đã chuyển đơn sang \"" + so.getStatusLabel() + "\".");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/orders/" + maDonCon;
    }

    /**
     * Chủ gian hàng làm được mọi việc; nhân viên phải được cấp quyền xử lý đơn.
     *
     * Interceptor chỉ biết "người này có thuộc gian hàng không", nó không biết
     * từng chức năng. Phân quyền chi tiết nằm ở controller vì nó phụ thuộc
     * việc đang làm (đặc tả mục 12.2).
     */
    private void batBuocQuyenDonHang(Long storeId, User u) {
        StoreMember tc = storeContext.batBuocThanhVien(storeId, u);
        if (!tc.isOwner() && !tc.mayHandleOrders()) {
            throw new LoiNghiepVu("Bạn không được phân quyền xử lý đơn hàng của gian hàng này.");
        }
    }
}
