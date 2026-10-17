package com.smartwash.product;

import com.smartwash.auth.AuthService;
import com.smartwash.category.Category;
import com.smartwash.category.CategoryService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Paginator;
import com.smartwash.store.Store;
import com.smartwash.store.StoreService;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/** Trang sản phẩm dành cho khách (đặc tả mục 5.1, 5.2, 5.3, 5.9). */
@Controller
public class ProductController {

    private final CatalogService catalogService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final StoreService storeService;
    private final AuthService authService;
    private final WishlistRepository wishlistRepository;

    public ProductController(CatalogService catalogService,
                             ProductService productService,
                             CategoryService categoryService,
                             StoreService storeService,
                             AuthService authService,
                             WishlistRepository wishlistRepository) {
        this.catalogService = catalogService;
        this.productService = productService;
        this.categoryService = categoryService;
        this.storeService = storeService;
        this.authService = authService;
        this.wishlistRepository = wishlistRepository;
    }

    // --------------------------------------------------------- danh sách

    @GetMapping("/products")
    public String danhSach(HttpServletRequest request,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) Long brandId,
                           @RequestParam(required = false) Long storeId,
                           @RequestParam(required = false) String doorType,
                           @RequestParam(required = false) Boolean inverter,
                           @RequestParam(required = false) BigDecimal capacityFrom,
                           @RequestParam(required = false) BigDecimal capacityTo,
                           @RequestParam(required = false) BigDecimal priceFrom,
                           @RequestParam(required = false) BigDecimal priceTo,
                           @RequestParam(required = false) String sort,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {

        CatalogService.BoLoc f = new CatalogService.BoLoc();
        f.q = q;
        f.categoryId = categoryId;
        f.brandId = brandId;
        f.storeId = storeId;
        f.doorType = doorType;
        f.inverter = inverter;
        f.capacityFrom = capacityFrom;
        f.capacityTo = capacityTo;
        f.priceFrom = priceFrom;
        f.priceTo = priceTo;
        f.sort = sort == null || sort.isBlank() ? CatalogService.MOI_NHAT : sort;

        model.addAttribute("ds", new Paginator<>(catalogService.tim(f, page, 24), request));
        model.addAttribute("loc", f);
        model.addAttribute("danhMuc", catalogService.danhMucLoc());
        model.addAttribute("thuongHieu", catalogService.thuongHieuLoc());
        model.addAttribute("khoangGia", catalogService.khoangGia());
        if (categoryId != null) {
            model.addAttribute("danhMucDangChon", categoryService.timHoacBao(categoryId));
        }
        return "san-pham/danh-sach";
    }

    /** Lối vào theo danh mục: /danh-muc/may-giat-cua-ngang */
    @GetMapping("/danh-muc/{slug}")
    public String theoDanhMuc(@PathVariable String slug) {
        Category c = categoryService.theoSlug(slug);
        return "redirect:/products?categoryId=" + c.getId();
    }

    // ---------------------------------------------------------- chi tiết

    @GetMapping("/products/{slug}")
    public String chiTiet(HttpServletRequest request, @PathVariable String slug, Model model) {
        Product p = productService.chiTietTheoSlug(slug);

        // Hàng chưa duyệt hoặc đã gỡ thì khách không xem được,
        // trừ quản trị viên và người của chính gian hàng đó (để xem thử).
        if (!p.isSellable()) {
            User u = authService.currentUser(request);
            boolean duocXem = u != null && (u.isAdmin()
                    || (p.getStore() != null
                        && storeService.cuaNguoiDung(u.getId()).stream()
                                .anyMatch(s -> s.getId().equals(p.getStore().getId()))));
            if (!duocXem) {
                throw new LoiNghiepVu("Sản phẩm này hiện không còn bán.");
            }
            model.addAttribute("xemThu", true);
        }

        catalogService.ghiLuotXem(p.getId());

        model.addAttribute("p", p);
        model.addAttribute("lienQuan", catalogService.gioiThieuThem(p, 4));

        User u = authService.currentUser(request);
        model.addAttribute("daThich",
                u != null && wishlistRepository.existsByUserIdAndProductId(u.getId(), p.getId()));
        return "san-pham/chi-tiet";
    }

    // ----------------------------------------------------- trang gian hàng

    @GetMapping("/stores/{slug}")
    public String gianHang(HttpServletRequest request,
                           @PathVariable String slug,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        Store s = storeService.theoSlug(slug);
        if (!s.isVisibleToPublic()) {
            throw new LoiNghiepVu("Gian hàng này hiện không hoạt động.");
        }

        CatalogService.BoLoc f = new CatalogService.BoLoc();
        f.storeId = s.getId();
        model.addAttribute("store", s);
        model.addAttribute("ds", new Paginator<>(catalogService.tim(f, page, 24), request));
        return "gian-hang/chi-tiet";
    }

    // ------------------------------------------------------------ yêu thích

    @PostMapping("/wishlist/{productId}")
    public String doiYeuThich(HttpServletRequest request,
                              @PathVariable Long productId,
                              @RequestParam(required = false) String next,
                              RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        wishlistRepository.findByUserIdAndProductId(u.getId(), productId)
                .ifPresentOrElse(
                        w -> {
                            wishlistRepository.delete(w);
                            ra.addFlashAttribute("success", "Đã bỏ khỏi danh sách yêu thích.");
                        },
                        () -> {
                            Wishlist w = new Wishlist();
                            w.setUser(u);
                            w.setProduct(productService.timHoacBao(productId));
                            wishlistRepository.save(w);
                            ra.addFlashAttribute("success", "Đã lưu vào danh sách yêu thích.");
                        });
        return "redirect:" + (next != null && next.startsWith("/") ? next : "/products");
    }

    @GetMapping("/wishlist")
    public String yeuThich(HttpServletRequest request,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        User u = authService.batBuocDangNhap(request);
        model.addAttribute("ds", new Paginator<>(
                wishlistRepository.cuaNguoiDung(u.getId(),
                        org.springframework.data.domain.PageRequest.of(Math.max(0, page - 1), 20)),
                request));
        return "tai-khoan/yeu-thich";
    }
}
