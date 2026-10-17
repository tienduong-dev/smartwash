package com.smartwash.product;

import com.smartwash.audit.AuditLog;
import com.smartwash.audit.AuditService;
import com.smartwash.brand.BrandNameNormalizer;
import com.smartwash.category.Category;
import com.smartwash.category.CategoryService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.LuuTru;
import com.smartwash.common.Messages;
import com.smartwash.inventory.Inventory;
import com.smartwash.notification.Notification;
import com.smartwash.notification.NotificationService;
import com.smartwash.setting.SettingService;
import com.smartwash.store.Store;
import com.smartwash.store.StoreMember;
import com.smartwash.store.StoreMemberRepository;
import com.smartwash.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sản phẩm: tạo, sửa, ảnh, thông số, kiểm duyệt
 * (đặc tả mục 4.4, 6.4, 6.5, 6.6, 7.6, BR-04, BR-07).
 *
 * Vòng đời:
 *   DRAFT ──nộp──► PENDING_REVIEW ──duyệt──► ACTIVE ⇄ INACTIVE
 *     ▲                   └──từ chối──► REJECTED ──sửa, nộp lại──┐
 *     └──────────────────────────────────────────────────────────┘
 *   Admin có thể gỡ hàng đang bán: ACTIVE ──► SUSPENDED
 */
@Service
public class ProductService {

    private final ProductRepository repo;
    private final ProductImageRepository imageRepository;
    private final ProductSpecRepository specRepository;
    private final CategoryService categoryService;
    private final StoreMemberRepository memberRepository;
    private final NotificationService notificationService;
    private final SettingService settingService;
    private final AuditService auditService;
    private final LuuTru luuTru;

    public ProductService(ProductRepository repo,
                          ProductImageRepository imageRepository,
                          ProductSpecRepository specRepository,
                          CategoryService categoryService,
                          StoreMemberRepository memberRepository,
                          NotificationService notificationService,
                          SettingService settingService,
                          AuditService auditService,
                          LuuTru luuTru) {
        this.repo = repo;
        this.imageRepository = imageRepository;
        this.specRepository = specRepository;
        this.categoryService = categoryService;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.settingService = settingService;
        this.auditService = auditService;
        this.luuTru = luuTru;
    }

    // =====================================================================
    // Tra cứu
    // =====================================================================

    public Product timHoacBao(Long id) {
        return repo.findDetailById(id).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy sản phẩm."));
    }

    public Product chiTietTheoSlug(String slug) {
        return repo.findDetailBySlug(slug).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy sản phẩm."));
    }

    /** Sản phẩm của gian hàng này, ném lỗi nếu lấy nhầm của gian khác. */
    public Product cuaGianHang(Long productId, Long storeId) {
        Product p = timHoacBao(productId);
        if (p.getStore() == null || !p.getStore().getId().equals(storeId)) {
            throw new LoiNghiepVu("Sản phẩm này không thuộc gian hàng của bạn.");
        }
        return p;
    }

    public Page<Product> danhSachGianHang(Long storeId, String tuKhoa, String trangThai,
                                          int trang, int moiTrang) {
        return repo.cuaGianHang(storeId, rong(tuKhoa), rong(trangThai),
                PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    public Page<Product> hangDoiDuyet(String tuKhoa, String trangThai, Long storeId,
                                      int trang, int moiTrang) {
        return repo.hangDoiDuyet(rong(tuKhoa), rong(trangThai), storeId,
                PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    public long soChoDuyet() {
        return repo.countByStatus(Product.PENDING_REVIEW);
    }

    // =====================================================================
    // Người bán: tạo và sửa
    // =====================================================================

    /**
     * Tạo sản phẩm mới ở trạng thái nháp.
     *
     * BR-04: thương hiệu KHÔNG phải tham số — lấy thẳng từ gian hàng.
     * Người bán không có cách nào đăng hàng thương hiệu khác.
     */
    @Transactional
    public Product tao(Store store, ThongTinSanPham form, User nguoiTao) {
        kiemTra(form, store.getId(), null);

        Product p = new Product();
        p.setStore(store);
        p.setBrand(store.getBrand());          // BR-04
        apDung(p, form);
        p.setSlug(slugDuyNhat(BrandNameNormalizer.slugify(
                form.name + "-" + form.modelCode), null));
        p.setStatus(Product.DRAFT);

        Inventory kho = new Inventory();
        kho.setProduct(p);
        kho.setQuantityAvailable(form.quantity == null ? 0 : Math.max(0, form.quantity));
        kho.setQuantityReserved(0);
        kho.setMinStock(form.minStock == null ? 0 : Math.max(0, form.minStock));
        kho.setCostPrice(form.costPrice == null ? 0L : Math.max(0L, form.costPrice));
        p.setInventory(kho);

        repo.save(p);
        thayThongSo(p, form.specKeys, form.specValues);
        return p;
    }

    @Transactional
    public Product capNhat(Long productId, Long storeId, ThongTinSanPham form) {
        Product p = cuaGianHang(productId, storeId);
        if (!p.isEditable()) {
            throw new LoiNghiepVu("Sản phẩm đang ở trạng thái \"" + p.getStatusLabel()
                    + "\" nên không sửa được. Chờ quản trị viên xử lý xong đã.");
        }
        kiemTra(form, storeId, productId);
        apDung(p, form);

        Inventory kho = p.getInventory();
        if (kho == null) {
            kho = new Inventory();
            kho.setProduct(p);
            p.setInventory(kho);
        }
        if (form.quantity != null) {
            kho.setQuantityAvailable(Math.max(0, form.quantity));
        }
        if (form.minStock != null) {
            kho.setMinStock(Math.max(0, form.minStock));
        }
        if (form.costPrice != null) {
            kho.setCostPrice(Math.max(0L, form.costPrice));
        }

        // Sửa hàng đang bán thì phải duyệt lại, tránh tráo nội dung sau khi duyệt
        if (Product.ACTIVE.equals(p.getStatus()) && settingService.batBuocDuyetSanPham()) {
            p.setStatus(Product.PENDING_REVIEW);
            p.setSubmittedAt(LocalDateTime.now());
        }

        repo.save(p);
        thayThongSo(p, form.specKeys, form.specValues);
        return p;
    }

    /** Nộp duyệt. Nếu sàn tắt bắt buộc duyệt thì cho bán luôn. */
    @Transactional
    public Product nopDuyet(Long productId, Long storeId, User nguoiNop, String ip) {
        Product p = cuaGianHang(productId, storeId);
        if (!p.canSubmit()) {
            throw new LoiNghiepVu("Sản phẩm đang ở trạng thái \"" + p.getStatusLabel()
                    + "\" nên không nộp duyệt được.");
        }
        if (p.getImages().isEmpty() && (p.getImage() == null || p.getImage().isBlank())) {
            throw new LoiNghiepVu("Sản phẩm cần có ít nhất một ảnh trước khi nộp duyệt.");
        }

        String cu = p.getStatus();
        p.setRejectReason(null);
        p.setSubmittedAt(LocalDateTime.now());

        if (settingService.batBuocDuyetSanPham()) {
            p.setStatus(Product.PENDING_REVIEW);
        } else {
            p.setStatus(Product.ACTIVE);
            p.setApprovedAt(LocalDateTime.now());
        }
        repo.save(p);

        auditService.ghiDoiTrangThai(nguoiNop, "PRODUCT_SUBMITTED", "Product", productId,
                cu, p.getStatus(), null, ip);
        return p;
    }

    /** Người bán bật/tắt bán. Chỉ đổi qua lại giữa ACTIVE và INACTIVE. */
    @Transactional
    public void doiBanHang(Long productId, Long storeId, boolean dangBan) {
        Product p = cuaGianHang(productId, storeId);
        if (!Product.ACTIVE.equals(p.getStatus()) && !Product.INACTIVE.equals(p.getStatus())) {
            throw new LoiNghiepVu("Chỉ bật/tắt được sản phẩm đã duyệt.");
        }
        p.setStatus(dangBan ? Product.ACTIVE : Product.INACTIVE);
        repo.save(p);
    }

    @Transactional
    public void xoa(Long productId, Long storeId) {
        Product p = cuaGianHang(productId, storeId);
        if (Product.ACTIVE.equals(p.getStatus()) || Product.PENDING_REVIEW.equals(p.getStatus())) {
            throw new LoiNghiepVu("Không xoá được sản phẩm đang bán hoặc đang chờ duyệt. "
                    + "Hãy tắt bán trước.");
        }
        for (ProductImage img : p.getImages()) {
            luuTru.xoaCongKhai(img.getPath());
        }
        repo.delete(p);
    }

    // =====================================================================
    // Ảnh
    // =====================================================================

    @Transactional
    public ProductImage themAnh(Long productId, Long storeId, MultipartFile file) {
        Product p = cuaGianHang(productId, storeId);
        if (imageRepository.countByProductId(productId) >= 10) {
            throw new LoiNghiepVu("Mỗi sản phẩm chỉ lưu tối đa 10 ảnh.");
        }

        String duongDan = luuTru.luuAnh(file, "products");
        ProductImage img = new ProductImage();
        img.setProduct(p);
        img.setPath(duongDan);
        img.setSortOrder((int) imageRepository.countByProductId(productId));
        // Ảnh đầu tiên mặc định là ảnh đại diện
        img.setIsPrimary(imageRepository.countByProductId(productId) == 0);
        imageRepository.save(img);

        if (Boolean.TRUE.equals(img.getIsPrimary())) {
            p.setImage(duongDan);
            repo.save(p);
        }
        return img;
    }

    @Transactional
    public void datAnhChinh(Long imageId, Long productId, Long storeId) {
        Product p = cuaGianHang(productId, storeId);
        ProductImage img = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy ảnh."));
        img.setIsPrimary(true);
        imageRepository.save(img);
        imageRepository.boAnhChinhKhac(productId, imageId);

        p.setImage(img.getPath());
        repo.save(p);
    }

    @Transactional
    public void xoaAnh(Long imageId, Long productId, Long storeId) {
        Product p = cuaGianHang(productId, storeId);
        ProductImage img = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy ảnh."));
        boolean laAnhChinh = img.isPrimaryImage();

        imageRepository.delete(img);
        luuTru.xoaCongKhai(img.getPath());

        // Xoá ảnh đại diện thì nâng ảnh còn lại lên thay
        if (laAnhChinh) {
            List<ProductImage> con = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId);
            if (con.isEmpty()) {
                p.setImage(null);
            } else {
                ProductImage moi = con.get(0);
                moi.setIsPrimary(true);
                imageRepository.save(moi);
                p.setImage(moi.getPath());
            }
            repo.save(p);
        }
    }

    // =====================================================================
    // Admin kiểm duyệt
    // =====================================================================

    @Transactional
    public void duyet(Long productId, User admin, String ip) {
        Product p = timHoacBao(productId);
        if (!Product.PENDING_REVIEW.equals(p.getStatus())) {
            throw new LoiNghiepVu("Chỉ duyệt được sản phẩm đang chờ duyệt.");
        }

        String cu = p.getStatus();
        p.setStatus(Product.ACTIVE);
        p.setRejectReason(null);
        p.setApprovedAt(LocalDateTime.now());
        p.setApprovedBy(admin);
        repo.save(p);

        baoChoGianHang(p, Notification.PRODUCT_APPROVED,
                "Sản phẩm \"" + p.getName() + "\" đã được duyệt",
                "Sản phẩm đã hiển thị với khách hàng.");

        auditService.ghiDoiTrangThai(admin, AuditLog.PRODUCT_APPROVED, "Product", productId,
                cu, Product.ACTIVE, null, ip);
    }

    @Transactional
    public void tuChoi(Long productId, String lyDo, User admin, String ip) {
        if (lyDo == null || lyDo.trim().isEmpty()) {
            throw new LoiNghiepVu("rejectReason", "Vui lòng nêu lý do từ chối để người bán biết đường sửa.");
        }
        Product p = timHoacBao(productId);
        if (!Product.PENDING_REVIEW.equals(p.getStatus())) {
            throw new LoiNghiepVu("Chỉ từ chối được sản phẩm đang chờ duyệt.");
        }

        String cu = p.getStatus();
        p.setStatus(Product.REJECTED);
        p.setRejectReason(lyDo.trim());
        repo.save(p);

        baoChoGianHang(p, Notification.PRODUCT_REJECTED,
                "Sản phẩm \"" + p.getName() + "\" chưa được duyệt", lyDo.trim());

        auditService.ghiDoiTrangThai(admin, AuditLog.PRODUCT_REJECTED, "Product", productId,
                cu, Product.REJECTED, lyDo.trim(), ip);
    }

    /** Gỡ hàng đang bán, ví dụ khi phát hiện vi phạm. */
    @Transactional
    public void go(Long productId, String lyDo, User admin, String ip) {
        Product p = timHoacBao(productId);
        String cu = p.getStatus();
        p.setStatus(Product.SUSPENDED);
        p.setRejectReason(lyDo);
        repo.save(p);

        baoChoGianHang(p, Notification.PRODUCT_REJECTED,
                "Sản phẩm \"" + p.getName() + "\" đã bị gỡ khỏi sàn",
                lyDo == null ? "Vui lòng liên hệ quản trị viên." : lyDo);

        auditService.ghiDoiTrangThai(admin, "PRODUCT_SUSPENDED", "Product", productId,
                cu, Product.SUSPENDED, lyDo, ip);
    }

    @Transactional
    public void phucHoi(Long productId, User admin, String ip) {
        Product p = timHoacBao(productId);
        if (!Product.SUSPENDED.equals(p.getStatus())) {
            throw new LoiNghiepVu("Chỉ phục hồi được sản phẩm đã bị gỡ.");
        }
        p.setStatus(Product.ACTIVE);
        p.setRejectReason(null);
        repo.save(p);
        auditService.ghiDoiTrangThai(admin, "PRODUCT_RESTORED", "Product", productId,
                Product.SUSPENDED, Product.ACTIVE, null, ip);
    }

    // =====================================================================
    // Kiểm tra dữ liệu
    // =====================================================================

    private void kiemTra(ThongTinSanPham f, Long storeId, Long boQuaId) {
        if (f.name == null || f.name.trim().isEmpty()) {
            throw new LoiNghiepVu("name", Messages.required("tên sản phẩm"));
        }
        if (f.modelCode == null || f.modelCode.trim().isEmpty()) {
            throw new LoiNghiepVu("modelCode", Messages.required("mã model"));
        }
        if (repo.trungModel(storeId, f.modelCode.trim(), boQuaId)) {
            throw new LoiNghiepVu("modelCode",
                    "Gian hàng đã có sản phẩm mã \"" + f.modelCode.trim() + "\".");
        }
        if (f.categoryId == null) {
            throw new LoiNghiepVu("categoryId", Messages.chon("danh mục"));
        }

        // BR-07
        if (f.price == null || f.price.signum() < 0) {
            throw new LoiNghiepVu("price", "Giá bán phải là số không âm.");
        }
        if (f.promoPrice != null && f.promoPrice.compareTo(f.price) > 0) {
            throw new LoiNghiepVu("promoPrice", "Giá khuyến mãi không được cao hơn giá bán.");
        }
        if (f.originalPrice != null && f.originalPrice.signum() < 0) {
            throw new LoiNghiepVu("originalPrice", "Giá gốc phải là số không âm.");
        }
        if (f.promoStartAt != null && f.promoEndAt != null && !f.promoStartAt.isBefore(f.promoEndAt)) {
            throw new LoiNghiepVu("promoEndAt", "Thời điểm kết thúc khuyến mãi phải sau thời điểm bắt đầu.");
        }
        if (f.doorType != null && !f.doorType.isBlank()
                && !List.of(Product.CUA_TREN, Product.CUA_NGANG).contains(f.doorType)) {
            throw new LoiNghiepVu("doorType", Messages.invalid("kiểu cửa"));
        }
        if (f.capacityKg != null && f.capacityKg.signum() <= 0) {
            throw new LoiNghiepVu("capacityKg", "Khối lượng giặt phải lớn hơn 0.");
        }
        if (f.warrantyMonths != null && f.warrantyMonths < 0) {
            throw new LoiNghiepVu("warrantyMonths", "Số tháng bảo hành không được âm.");
        }
    }

    private void apDung(Product p, ThongTinSanPham f) {
        Category dm = categoryService.timHoacBao(f.categoryId);
        p.setCategory(dm);
        p.setModelCode(f.modelCode.trim());
        p.setName(f.name.trim());
        p.setDescription(f.description);
        p.setHighlights(f.highlights);
        p.setPrice(f.price);
        p.setOriginalPrice(f.originalPrice);
        p.setPromoPrice(f.promoPrice);
        p.setPromoStartAt(f.promoStartAt);
        p.setPromoEndAt(f.promoEndAt);
        p.setWarrantyMonths(f.warrantyMonths == null ? 12 : f.warrantyMonths);
        p.setDoorType(f.doorType == null || f.doorType.isBlank() ? null : f.doorType);
        p.setCapacityKg(f.capacityKg);
        p.setSpinSpeedRpm(f.spinSpeedRpm);
        p.setIsInverter(Boolean.TRUE.equals(f.isInverter));
        p.setEnergyRating(f.energyRating);
        p.setWidthCm(f.widthCm);
        p.setHeightCm(f.heightCm);
        p.setDepthCm(f.depthCm);
        p.setWeightKg(f.weightKg);
        p.setPowerKwhYear(f.powerKwhYear);
        p.setWaterLiterCycle(f.waterLiterCycle);
    }

    /** Thay toàn bộ thông số phụ bằng danh sách mới. */
    @Transactional
    public void thayThongSo(Product p, List<String> khoa, List<String> giaTri) {
        if (khoa == null || giaTri == null) {
            return;
        }
        specRepository.deleteByProductId(p.getId());
        p.getSpecs().clear();

        // Gộp theo khoá: bảng có UNIQUE (product_id, spec_key), nhập trùng
        // sẽ vỡ ở CSDL nên loại ngay tại đây, lấy giá trị nhập sau cùng.
        Map<String, String> gop = new LinkedHashMap<>();
        for (int i = 0; i < khoa.size() && i < giaTri.size(); i++) {
            String k = khoa.get(i) == null ? "" : khoa.get(i).trim();
            String v = giaTri.get(i) == null ? "" : giaTri.get(i).trim();
            if (!k.isEmpty() && !v.isEmpty()) {
                gop.put(k, v);
            }
        }

        int thuTu = 0;
        for (Map.Entry<String, String> e : gop.entrySet()) {
            ProductSpec s = new ProductSpec(e.getKey(), e.getValue(), thuTu++);
            s.setProduct(p);
            specRepository.save(s);
            p.getSpecs().add(s);
        }
    }

    // =====================================================================
    // Nội bộ
    // =====================================================================

    private void baoChoGianHang(Product p, String loai, String tieuDe, String noiDung) {
        if (p.getStore() == null) {
            return;
        }
        Long storeId = p.getStore().getId();
        for (StoreMember m : memberRepository.chuGianHang(storeId)) {
            notificationService.gui(m.getUser(), loai, tieuDe, noiDung,
                    "/seller/" + storeId + "/products/" + p.getId());
        }
    }

    private String slugDuyNhat(String goc, Long boQuaId) {
        String s = goc == null || goc.isBlank() ? "san-pham" : goc;
        if (!repo.existsBySlug(s)) {
            return s;
        }
        for (int i = 2; i < 500; i++) {
            String thu = s + "-" + i;
            if (!repo.existsBySlug(thu)) {
                return thu;
            }
        }
        return s + "-" + System.currentTimeMillis();
    }

    private String rong(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // =====================================================================
    // Gói dữ liệu form — gom cho gọn, controller khỏi truyền 25 tham số
    // =====================================================================

    public static class ThongTinSanPham {
        public Long categoryId;
        public String modelCode;
        public String name;
        public String description;
        public String highlights;
        public BigDecimal price;
        public BigDecimal originalPrice;
        public BigDecimal promoPrice;
        public LocalDateTime promoStartAt;
        public LocalDateTime promoEndAt;
        public Integer warrantyMonths;
        public String doorType;
        public BigDecimal capacityKg;
        public Integer spinSpeedRpm;
        public Boolean isInverter;
        public String energyRating;
        public BigDecimal widthCm;
        public BigDecimal heightCm;
        public BigDecimal depthCm;
        public BigDecimal weightKg;
        public BigDecimal powerKwhYear;
        public BigDecimal waterLiterCycle;
        public Integer quantity;
        public Integer minStock;
        public Long costPrice;
        public List<String> specKeys;
        public List<String> specValues;
    }
}
