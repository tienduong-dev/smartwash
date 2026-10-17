package com.smartwash.sellerregistration;

import com.smartwash.audit.AuditService;
import com.smartwash.auth.AuthService;
import com.smartwash.brand.BrandService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.LuuTru;
import com.smartwash.common.OldInput;
import com.smartwash.common.Req;
import com.smartwash.store.StoreService;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Khu người bán: nộp và theo dõi hồ sơ mở gian hàng (đặc tả mục 6.1, 7.4). */
@Controller
public class SellerRegistrationController {

    private final AuthService authService;
    private final SellerRegistrationService registrationService;
    private final SellerDocumentRepository documentRepository;
    private final BrandService brandService;
    private final StoreService storeService;
    private final LuuTru luuTru;

    public SellerRegistrationController(AuthService authService,
                                        SellerRegistrationService registrationService,
                                        SellerDocumentRepository documentRepository,
                                        BrandService brandService,
                                        StoreService storeService,
                                        LuuTru luuTru) {
        this.authService = authService;
        this.registrationService = registrationService;
        this.documentRepository = documentRepository;
        this.brandService = brandService;
        this.storeService = storeService;
        this.luuTru = luuTru;
    }

    /** Trang chính khu người bán: gian hàng đang có + hồ sơ đã nộp. */
    @GetMapping("/seller")
    public String trangChinh(HttpServletRequest request, Model model) {
        User u = authService.batBuocDangNhap(request);
        model.addAttribute("gianHang", storeService.cuaNguoiDung(u.getId()));
        model.addAttribute("hoSo", registrationService.cuaNguoiDung(u.getId()));
        return "nguoi-ban/trang-chinh";
    }

    @GetMapping("/seller/register")
    public String formNop(HttpServletRequest request, Model model) {
        authService.batBuocDangNhap(request);
        model.addAttribute("thuongHieu", brandService.choChonHoSo());
        return "nguoi-ban/nop-ho-so";
    }

    @PostMapping("/seller/register")
    public String nop(HttpServletRequest request,
                      @RequestParam(required = false) Long brandId,
                      @RequestParam(required = false) String brandMoi,
                      @RequestParam String storeName,
                      @RequestParam String storeType,
                      @RequestParam(required = false) String contactPhone,
                      @RequestParam(required = false) String contactEmail,
                      @RequestParam(required = false) String pickupAddress,
                      @RequestParam(required = false) String note,
                      Model model,
                      RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            SellerRegistration r = registrationService.nop(
                    u, null, brandId, brandMoi, storeName, storeType,
                    contactPhone, contactEmail, pickupAddress, note,
                    AuditService.ipCua(request));
            ra.addFlashAttribute("success",
                    "Đã nộp hồ sơ " + r.getCode() + ". Vui lòng tải lên giấy tờ để chúng tôi thẩm định.");
            return "redirect:/seller/registration/" + r.getId();
        } catch (LoiNghiepVu e) {
            model.addAttribute("thuongHieu", brandService.choChonHoSo());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("old", new OldInput(request.getParameterMap()));
            return "nguoi-ban/nop-ho-so";
        }
    }

    @GetMapping("/seller/registration/{id}")
    public String chiTiet(HttpServletRequest request, @PathVariable Long id, Model model) {
        User u = authService.batBuocDangNhap(request);
        SellerRegistration r = registrationService.chiTiet(id);
        if (!r.getApplicant().getId().equals(u.getId()) && !u.isAdmin()) {
            throw new LoiNghiepVu("Bạn không có quyền xem hồ sơ này.");
        }
        model.addAttribute("hoSo", r);
        return "nguoi-ban/chi-tiet-ho-so";
    }

    @PostMapping("/seller/registration/{id}/documents")
    public String taiGiayTo(HttpServletRequest request,
                            @PathVariable Long id,
                            @RequestParam String documentType,
                            @RequestParam MultipartFile file,
                            RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            String duongDan = luuTru.luuTaiLieu(file, "ho-so");
            registrationService.themGiayTo(id, u, documentType, duongDan,
                    file.getOriginalFilename(), file.getContentType(), file.getSize());
            ra.addFlashAttribute("success", "Đã tải lên giấy tờ.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller/registration/" + id;
    }

    @PostMapping("/seller/registration/{id}/cancel")
    public String huy(HttpServletRequest request, @PathVariable Long id, RedirectAttributes ra) {
        User u = authService.batBuocDangNhap(request);
        try {
            registrationService.huy(id, u, AuditService.ipCua(request));
            ra.addFlashAttribute("success", "Đã huỷ hồ sơ.");
        } catch (LoiNghiepVu e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller";
    }

    /**
     * Tải giấy tờ về. File nằm ngoài webroot nên phải đi qua đây để kiểm tra
     * quyền: chỉ người nộp hoặc Admin xem được (đặc tả mục 12.1).
     */
    @GetMapping("/seller/documents/{docId}")
    public ResponseEntity<InputStreamResource> taiVe(HttpServletRequest request, @PathVariable Long docId) {
        User u = authService.batBuocDangNhap(request);
        SellerDocument d = documentRepository.findById(docId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy giấy tờ."));

        boolean laChuHoSo = d.getRegistration().getApplicant().getId().equals(u.getId());
        if (!laChuHoSo && !u.isAdmin()) {
            throw new LoiNghiepVu("Bạn không có quyền xem giấy tờ này.");
        }

        String ten = d.getOriginalName() == null ? "giay-to" : d.getOriginalName();
        String tenMaHoa = URLEncoder.encode(ten, StandardCharsets.UTF_8).replace("+", "%20");
        MediaType loai = d.getMimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(d.getMimeType());

        return ResponseEntity.ok()
                .contentType(loai)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + tenMaHoa)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new InputStreamResource(luuTru.docRiengTu(d.getFilePath())));
    }
}
