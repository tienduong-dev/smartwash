package com.smartwash.product;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.category.CategoryService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.OldInput;
import com.smartwash.common.Paginator;
import com.smartwash.inventory.InventoryService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Khu người bán: quản lý sản phẩm của một gian hàng (đặc tả mục 6.4, 6.5, 6.6).
 *
 * BR-04 thể hiện ngay ở đây: form không có ô chọn thương hiệu. Thương hiệu
 * lấy từ gian hàng, người bán chỉ nhìn chứ không sửa được.
 */
@Controller
@RequestMapping("/seller/{storeId}/products")
public class SellerProductController {

    private final AuthService authService;
    private final StoreContext storeContext;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;

    public SellerProductController(AuthService authService,
                                   StoreContext storeContext,
                                   ProductService productService,
                                   CategoryService categoryService,
                                   InventoryService inventoryService) {
        this.authService = authService;
        this.storeContext = storeContext;
        this.productService = productService;
        this.categoryService = categoryService;
        this.inventoryService = inventoryService;
    }

    // ------------------------------------------------------------ danh sách

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @PathVariable Long storeId,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        User u = authService.batBuocDangNhap(request);
        Store s = storeContext.batBuocGianHang(storeId, u);

        model.addAttribute("store", s);
        model.addAttribute("ds", new Paginator<>(
                productService.danhSachGianHang(storeId, q, status, page, 20), request));
        model.addAttribute("soSapHet", inventoryService.demSapHet(storeId));
        model.addAttribute("soHetHang", inventoryService.demHetHang(storeId));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "nguoi-ban/san-pham";
    }

    // --------------------------------------------------------- form tạo/sửa

    @GetMapping("/new")
    public String formTao(HttpServletRequest request, @PathVariable Long storeId, Model model) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);

        model.addAttribute("store", storeContext.batBuocGianHang(storeId, u));
        model.addAttribute("danhMuc", categoryService.choChon());
        model.addAttribute("p", null);
        return "nguoi-ban/sua-san-pham";
    }

    @GetMapping("/{productId}")
    public String formSua(HttpServletRequest request,
                          @PathVariable Long storeId,
                          @PathVariable Long productId,
                          Model model) {
        User u = authService.batBuocDangNhap(request);
        storeContext.batBuocThanhVien(storeId, u);

        model.addAttribute("store", storeContext.batBuocGianHang(storeId, u));
        model.addAttribute("danhMuc", categoryService.choChon());
        model.addAttribute("p", productService.cuaGianHang(productId, storeId));
        return "nguoi-ban/sua-san-pham";
    }

    @PostMapping
    public String tao(HttpServletRequest request, @PathVariable Long storeId,
                      Model model, RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        Store s = storeContext.batBuocGianHang(storeId, u);

        try {
            Product p = productService.tao(s, docForm(request), u);
            ra.addFlashAttribute("success",
                    "Đã tạo sản phẩm ở dạng nháp. Thêm ảnh rồi nộp duyệt để bắt đầu bán.");
            return "redirect:/seller/" + storeId + "/products/" + p.getId();
        } catch (LoiNghiepVu e) {
            model.addAttribute("store", s);
            model.addAttribute("danhMuc", categoryService.choChon());
            model.addAttribute("p", null);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("old", new OldInput(request.getParameterMap()));
            return "nguoi-ban/sua-san-pham";
        }
    }

    @PostMapping("/{productId}")
    public String capNhat(HttpServletRequest request,
                          @PathVariable Long storeId,
                          @PathVariable Long productId,
                          RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        try {
            productService.capNhat(productId, storeId, docForm(request));
            ra.addFlashAttribute("success", "Đã lưu sản phẩm.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/products/" + productId;
    }

    // ------------------------------------------------------------ hành động

    @PostMapping("/{productId}/submit")
    public String nopDuyet(HttpServletRequest request,
                           @PathVariable Long storeId,
                           @PathVariable Long productId,
                           RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        try {
            Product p = productService.nopDuyet(productId, storeId, u, AuditService.ipCua(request));
            ra.addFlashAttribute("success", Product.ACTIVE.equals(p.getStatus())
                    ? "Sản phẩm đã lên sàn."
                    : "Đã nộp duyệt. Quản trị viên sẽ xem trong thời gian sớm nhất.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/products/" + productId;
    }

    @PostMapping("/{productId}/selling")
    public String doiBanHang(HttpServletRequest request,
                             @PathVariable Long storeId,
                             @PathVariable Long productId,
                             @RequestParam boolean selling,
                             RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        try {
            productService.doiBanHang(productId, storeId, selling);
            ra.addFlashAttribute("success", selling ? "Đã mở bán lại." : "Đã tạm ngừng bán.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/products/" + productId;
    }

    @PostMapping("/{productId}/delete")
    public String xoa(HttpServletRequest request,
                      @PathVariable Long storeId,
                      @PathVariable Long productId,
                      RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        try {
            productService.xoa(productId, storeId);
            ra.addFlashAttribute("success", "Đã xoá sản phẩm.");
            return "redirect:/seller/" + storeId + "/products";
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/seller/" + storeId + "/products/" + productId;
        }
    }

    // ----------------------------------------------------------------- ảnh

    @PostMapping("/{productId}/images")
    public String themAnh(HttpServletRequest request,
                          @PathVariable Long storeId,
                          @PathVariable Long productId,
                          @RequestParam MultipartFile file,
                          RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        try {
            productService.themAnh(productId, storeId, file);
            ra.addFlashAttribute("success", "Đã thêm ảnh.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/products/" + productId;
    }

    @PostMapping("/{productId}/images/{imageId}/primary")
    public String datAnhChinh(HttpServletRequest request,
                              @PathVariable Long storeId,
                              @PathVariable Long productId,
                              @PathVariable Long imageId,
                              RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        try {
            productService.datAnhChinh(imageId, productId, storeId);
            ra.addFlashAttribute("success", "Đã đặt ảnh đại diện.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/products/" + productId;
    }

    @PostMapping("/{productId}/images/{imageId}/delete")
    public String xoaAnh(HttpServletRequest request,
                         @PathVariable Long storeId,
                         @PathVariable Long productId,
                         @PathVariable Long imageId,
                         RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        try {
            productService.xoaAnh(imageId, productId, storeId);
            ra.addFlashAttribute("success", "Đã xoá ảnh.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/products/" + productId;
    }

    // ------------------------------------------------------------- tồn kho

    @PostMapping("/{productId}/stock")
    public String datTon(HttpServletRequest request,
                         @PathVariable Long storeId,
                         @PathVariable Long productId,
                         @RequestParam int quantity,
                         @RequestParam(required = false) Integer minStock,
                         @RequestParam(required = false) Long costPrice,
                         RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        batBuocQuyenSanPham(storeId, u);
        try {
            inventoryService.datTon(productId, storeId, quantity, minStock, costPrice);
            ra.addFlashAttribute("success", "Đã cập nhật tồn kho.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/" + storeId + "/products/" + productId;
    }

    // ------------------------------------------------------------- nội bộ

    private void batBuocQuyenSanPham(Long storeId, User u) {
        StoreMember m = storeContext.batBuocThanhVien(storeId, u);
        if (!m.mayManageProducts()) {
            throw new LoiNghiepVu("Bạn không được phân quyền quản lý sản phẩm của gian hàng này.");
        }
    }

    /** Đọc form thành gói dữ liệu. Ô trống trả null chứ không phải chuỗi rỗng. */
    private ProductService.ThongTinSanPham docForm(HttpServletRequest r) {
        ProductService.ThongTinSanPham f = new ProductService.ThongTinSanPham();
        f.categoryId      = soDai(r.getParameter("categoryId"));
        f.modelCode       = r.getParameter("modelCode");
        f.name            = r.getParameter("name");
        f.description     = chuoi(r.getParameter("description"));
        f.highlights      = chuoi(r.getParameter("highlights"));
        f.price           = tien(r.getParameter("price"));
        f.originalPrice   = tien(r.getParameter("originalPrice"));
        f.promoPrice      = tien(r.getParameter("promoPrice"));
        f.promoStartAt    = thoiDiem(r.getParameter("promoStartAt"));
        f.promoEndAt      = thoiDiem(r.getParameter("promoEndAt"));
        f.warrantyMonths  = soNguyen(r.getParameter("warrantyMonths"));
        f.doorType        = chuoi(r.getParameter("doorType"));
        f.capacityKg      = thapPhan(r.getParameter("capacityKg"));
        f.spinSpeedRpm    = soNguyen(r.getParameter("spinSpeedRpm"));
        f.isInverter      = "true".equals(r.getParameter("isInverter"))
                            || "on".equals(r.getParameter("isInverter"));
        f.energyRating    = chuoi(r.getParameter("energyRating"));
        f.widthCm         = thapPhan(r.getParameter("widthCm"));
        f.heightCm        = thapPhan(r.getParameter("heightCm"));
        f.depthCm         = thapPhan(r.getParameter("depthCm"));
        f.weightKg        = thapPhan(r.getParameter("weightKg"));
        f.powerKwhYear    = thapPhan(r.getParameter("powerKwhYear"));
        f.waterLiterCycle = thapPhan(r.getParameter("waterLiterCycle"));
        f.quantity        = soNguyen(r.getParameter("quantity"));
        f.minStock        = soNguyen(r.getParameter("minStock"));
        f.costPrice       = soDai(r.getParameter("costPrice"));

        String[] k = r.getParameterValues("specKey");
        String[] v = r.getParameterValues("specValue");
        f.specKeys   = k == null ? null : List.of(k);
        f.specValues = v == null ? null : List.of(v);
        return f;
    }

    private String chuoi(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private Long soDai(String s) {
        try {
            return chuoi(s) == null ? null : Long.parseLong(s.trim().replace(".", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer soNguyen(String s) {
        try {
            return chuoi(s) == null ? null : Integer.parseInt(s.trim().replace(".", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Tiền nhập có thể có dấu chấm phân nhóm: "13.410.000" -> 13410000 */
    private BigDecimal tien(String s) {
        try {
            return chuoi(s) == null ? null : new BigDecimal(s.trim().replace(".", "").replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Số thập phân dùng dấu chấm: "10.5" */
    private BigDecimal thapPhan(String s) {
        try {
            return chuoi(s) == null ? null : new BigDecimal(s.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime thoiDiem(String s) {
        try {
            return chuoi(s) == null ? null : LocalDateTime.parse(s.trim());
        } catch (RuntimeException e) {
            return null;
        }
    }
}
