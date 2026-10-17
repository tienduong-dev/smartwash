package com.smartwash.admin;

import com.smartwash.category.CategoryService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.LuuTru;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** Quản trị danh mục (đặc tả mục 4.4, 7.6). */
@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final LuuTru luuTru;

    public AdminCategoryController(CategoryService categoryService, LuuTru luuTru) {
        this.categoryService = categoryService;
        this.luuTru = luuTru;
    }

    @GetMapping
    public String danhSach(Model model) {
        List<com.smartwash.category.Category> cay = categoryService.cay();
        categoryService.napSoSanPham(cay);
        model.addAttribute("cay", cay);
        model.addAttribute("choChon", categoryService.choChon());
        return "admin/danh-muc";
    }

    @PostMapping
    public String tao(@RequestParam String name,
                      @RequestParam(required = false) Long parentId,
                      @RequestParam(required = false) String description,
                      @RequestParam(required = false) Integer sortOrder,
                      @RequestParam(defaultValue = "true") boolean active,
                      @RequestParam(required = false) MultipartFile image,
                      RedirectAttributes ra) {
        try {
            String anh = image != null && !image.isEmpty() ? luuTru.luuAnh(image, "categories") : null;
            categoryService.tao(name, parentId, description, anh, sortOrder, active);
            ra.addFlashAttribute("success", "Đã thêm danh mục.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String capNhat(@PathVariable Long id,
                          @RequestParam String name,
                          @RequestParam(required = false) Long parentId,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) Integer sortOrder,
                          @RequestParam(defaultValue = "false") boolean active,
                          @RequestParam(required = false) MultipartFile image,
                          RedirectAttributes ra) {
        try {
            String anh = image != null && !image.isEmpty() ? luuTru.luuAnh(image, "categories") : null;
            categoryService.capNhat(id, name, parentId, description, anh, sortOrder, active);
            ra.addFlashAttribute("success", "Đã lưu danh mục.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String xoa(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.xoa(id);
            ra.addFlashAttribute("success", "Đã xoá danh mục.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/visibility")
    public String doiHien(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes ra) {
        categoryService.doiTrangThai(id, active);
        ra.addFlashAttribute("success", active ? "Đã hiện danh mục." : "Đã ẩn danh mục.");
        return "redirect:/admin/categories";
    }
}
