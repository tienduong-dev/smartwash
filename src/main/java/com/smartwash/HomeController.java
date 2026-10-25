package com.smartwash;

import com.smartwash.brand.BrandRepository;
import com.smartwash.category.CategoryService;
import com.smartwash.product.CatalogService;
import com.smartwash.store.StoreRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Trang chủ và trang danh sách gian hàng. */
@Controller
public class HomeController {

    private final StoreRepository storeRepository;
    private final BrandRepository brandRepository;
    private final CatalogService catalogService;
    private final CategoryService categoryService;

    public HomeController(StoreRepository storeRepository,
                          BrandRepository brandRepository,
                          CatalogService catalogService,
                          CategoryService categoryService) {
        this.storeRepository = storeRepository;
        this.brandRepository = brandRepository;
        this.catalogService = catalogService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String trangChu(Model model) {
        model.addAttribute("noiBat", catalogService.noiBat(8));
        model.addAttribute("danhMuc", categoryService.cayHien());
        model.addAttribute("gianHang", storeRepository.congKhai(PageRequest.of(0, 8)).getContent());
        model.addAttribute("thuongHieu", brandRepository.findByStatusOrderByNameAsc("ACTIVE"));
        return "trang-chu";
    }

    @GetMapping("/stores")
    public String danhSachGianHang(Model model) {
        model.addAttribute("gianHang", storeRepository.congKhai(PageRequest.of(0, 60)).getContent());
        return "gian-hang/danh-sach";
    }

    /**
     * Trang hỗ trợ — nội dung tĩnh, không cần đăng nhập.
     *
     * Trả lời sẵn những câu khách hay hỏi trước khi mua: hàng có chính hãng
     * không, bao lâu thì giao, lỗi thì đổi thế nào. Đỡ cho bộ phận hỗ trợ và
     * cũng là chỗ menu "Hỗ trợ" trỏ tới.
     */
    @GetMapping("/support")
    public String hoTro() {
        return "ho-tro/index";
    }
}
