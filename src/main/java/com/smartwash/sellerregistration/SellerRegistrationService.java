package com.smartwash.sellerregistration;

import com.smartwash.audit.AuditLog;
import com.smartwash.audit.AuditService;
import com.smartwash.brand.Brand;
import com.smartwash.brand.BrandNameNormalizer;
import com.smartwash.brand.BrandRepository;
import com.smartwash.brand.BrandService;
import com.smartwash.business.Business;
import com.smartwash.business.BusinessRepository;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Messages;
import com.smartwash.notification.Notification;
import com.smartwash.notification.NotificationService;
import com.smartwash.store.Store;
import com.smartwash.store.StoreMember;
import com.smartwash.store.StoreMemberRepository;
import com.smartwash.store.StoreRepository;
import com.smartwash.user.Role;
import com.smartwash.user.User;
import com.smartwash.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hồ sơ mở gian hàng (đặc tả mục 4.2, 6.1, 7.4, BR-02..BR-06).
 *
 * Trạng thái:
 *   PENDING ──► UNDER_REVIEW ──┬─► NEEDS_MORE_INFO ──► UNDER_REVIEW
 *                              ├─► APPROVED   (tạo Store + gắn Brand + cấp quyền)
 *                              └─► REJECTED
 *   Người nộp có thể CANCELLED khi hồ sơ chưa chốt.
 */
@Service
public class SellerRegistrationService {

    private final SellerRegistrationRepository regRepo;
    private final SellerDocumentRepository docRepo;
    private final BrandRepository brandRepo;
    private final BrandService brandService;
    private final BusinessRepository businessRepo;
    private final StoreRepository storeRepo;
    private final StoreMemberRepository memberRepo;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public SellerRegistrationService(SellerRegistrationRepository regRepo,
                                     SellerDocumentRepository docRepo,
                                     BrandRepository brandRepo,
                                     BrandService brandService,
                                     BusinessRepository businessRepo,
                                     StoreRepository storeRepo,
                                     StoreMemberRepository memberRepo,
                                     UserService userService,
                                     NotificationService notificationService,
                                     AuditService auditService) {
        this.regRepo = regRepo;
        this.docRepo = docRepo;
        this.brandRepo = brandRepo;
        this.brandService = brandService;
        this.businessRepo = businessRepo;
        this.storeRepo = storeRepo;
        this.memberRepo = memberRepo;
        this.userService = userService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // =====================================================================
    // Người bán nộp hồ sơ
    // =====================================================================

    /**
     * Nộp hồ sơ mới.
     *
     * @param brandId        thương hiệu có sẵn, hoặc null
     * @param tenBrandMoi    tên thương hiệu đề xuất khi brandId null
     */
    @Transactional
    public SellerRegistration nop(User nguoiNop,
                                  Long businessId,
                                  Long brandId,
                                  String tenBrandMoi,
                                  String tenGianHang,
                                  String loaiGianHang,
                                  String dienThoai,
                                  String email,
                                  String diaChiLayHang,
                                  String ghiChu,
                                  String ip) {

        if (regRepo.dangCoHoSoCho(nguoiNop.getId())) {
            throw new LoiNghiepVu("Bạn đang có một hồ sơ chờ xử lý. "
                    + "Vui lòng chờ kết quả hoặc huỷ hồ sơ cũ trước khi nộp hồ sơ mới.");
        }
        if (tenGianHang == null || tenGianHang.trim().isEmpty()) {
            throw new LoiNghiepVu("storeName", Messages.required("tên gian hàng"));
        }
        if (brandId == null && (tenBrandMoi == null || tenBrandMoi.trim().isEmpty())) {
            throw new LoiNghiepVu("brand", "Vui lòng chọn thương hiệu có sẵn hoặc nhập tên thương hiệu mới.");
        }
        if (!List.of(SellerRegistration.OFFICIAL, SellerRegistration.DISTRIBUTOR, SellerRegistration.RETAILER)
                .contains(loaiGianHang)) {
            throw new LoiNghiepVu("storeType", Messages.invalid("loại gian hàng"));
        }

        SellerRegistration r = new SellerRegistration();
        r.setCode(sinhMa());
        r.setApplicant(nguoiNop);
        if (businessId != null) {
            r.setBusiness(businessRepo.findById(businessId)
                    .orElseThrow(() -> new LoiNghiepVu("business", "Không tìm thấy doanh nghiệp đã chọn.")));
        }
        if (brandId != null) {
            Brand b = brandRepo.findById(brandId)
                    .orElseThrow(() -> new LoiNghiepVu("brand", "Không tìm thấy thương hiệu đã chọn."));
            if (!b.isActive()) {
                throw new LoiNghiepVu("brand", "Thương hiệu này hiện không nhận đăng ký mới.");
            }
            r.setRequestedBrand(b);
        } else {
            r.setRequestedBrandName(tenBrandMoi.trim());
        }
        r.setStoreName(tenGianHang.trim());
        r.setStoreSlugProposed(BrandNameNormalizer.slugify(tenGianHang));
        r.setStoreType(loaiGianHang);
        r.setContactPhone(dienThoai);
        r.setContactEmail(email);
        r.setPickupAddress(diaChiLayHang);
        r.setApplicantNote(ghiChu);
        r.setStatus(SellerRegistration.PENDING);
        regRepo.save(r);

        auditService.ghi(nguoiNop, "REGISTRATION_SUBMITTED", "SellerRegistration", r.getId(), null,
                Map.of("code", r.getCode(), "storeName", r.getStoreName()), null, ip);
        return r;
    }

    /** Người nộp bổ sung giấy tờ khi hồ sơ ở trạng thái cần bổ sung. */
    @Transactional
    public SellerDocument themGiayTo(Long hoSoId, User nguoiNop, String loaiGiayTo,
                                     String duongDan, String tenGoc, String mimeType, Long kichThuoc) {
        SellerRegistration r = timHoacBao(hoSoId);
        if (!r.getApplicant().getId().equals(nguoiNop.getId())) {
            throw new LoiNghiepVu("Bạn không có quyền với hồ sơ này.");
        }
        if (!r.canSupplement()) {
            throw new LoiNghiepVu("Hồ sơ ở trạng thái \"" + r.getStatusLabel() + "\" nên không nộp thêm giấy tờ được.");
        }

        SellerDocument d = new SellerDocument();
        d.setRegistration(r);
        d.setDocumentType(loaiGiayTo);
        d.setFilePath(duongDan);
        d.setOriginalName(tenGoc);
        d.setMimeType(mimeType);
        d.setSizeBytes(kichThuoc);
        d.setStatus(SellerDocument.PENDING);
        docRepo.save(d);

        // Bổ sung xong thì đưa hồ sơ về hàng đợi thẩm định
        if (SellerRegistration.NEEDS_MORE_INFO.equals(r.getStatus())) {
            r.setStatus(SellerRegistration.UNDER_REVIEW);
            regRepo.save(r);
        }
        return d;
    }

    @Transactional
    public void huy(Long hoSoId, User nguoiNop, String ip) {
        SellerRegistration r = timHoacBao(hoSoId);
        if (!r.getApplicant().getId().equals(nguoiNop.getId())) {
            throw new LoiNghiepVu("Bạn không có quyền với hồ sơ này.");
        }
        if (!r.canCancel()) {
            throw new LoiNghiepVu("Hồ sơ đã chốt, không huỷ được.");
        }
        String cu = r.getStatus();
        r.setStatus(SellerRegistration.CANCELLED);
        regRepo.save(r);
        auditService.ghiDoiTrangThai(nguoiNop, "REGISTRATION_CANCELLED", "SellerRegistration", hoSoId,
                cu, SellerRegistration.CANCELLED, null, ip);
    }

    // =====================================================================
    // Admin thẩm định
    // =====================================================================

    @Transactional
    public void tiepNhan(Long hoSoId, User admin, String ip) {
        SellerRegistration r = timHoacBao(hoSoId);
        batBuocDangXuLy(r);
        String cu = r.getStatus();
        r.setStatus(SellerRegistration.UNDER_REVIEW);
        r.setReviewedBy(admin);
        regRepo.save(r);
        auditService.ghiDoiTrangThai(admin, "REGISTRATION_UNDER_REVIEW", "SellerRegistration", hoSoId,
                cu, SellerRegistration.UNDER_REVIEW, null, ip);
    }

    @Transactional
    public void yeuCauBoSung(Long hoSoId, String noiDungCanBoSung, User admin, String ip) {
        if (noiDungCanBoSung == null || noiDungCanBoSung.trim().isEmpty()) {
            throw new LoiNghiepVu("reviewNote", "Vui lòng nêu rõ cần bổ sung những gì.");
        }
        SellerRegistration r = timHoacBao(hoSoId);
        batBuocDangXuLy(r);

        String cu = r.getStatus();
        r.setStatus(SellerRegistration.NEEDS_MORE_INFO);
        r.setReviewNote(noiDungCanBoSung.trim());
        r.setReviewedBy(admin);
        r.setReviewedAt(LocalDateTime.now());
        regRepo.save(r);

        notificationService.gui(r.getApplicant(), Notification.STORE_NEEDS_INFO,
                "Hồ sơ " + r.getCode() + " cần bổ sung",
                noiDungCanBoSung.trim(),
                "/seller/registration/" + r.getId());

        auditService.ghiDoiTrangThai(admin, "REGISTRATION_NEEDS_INFO", "SellerRegistration", hoSoId,
                cu, SellerRegistration.NEEDS_MORE_INFO, noiDungCanBoSung.trim(), ip);
    }

    @Transactional
    public void tuChoi(Long hoSoId, String lyDo, User admin, String ip) {
        if (lyDo == null || lyDo.trim().isEmpty()) {
            throw new LoiNghiepVu("reviewNote", "Vui lòng nêu lý do từ chối.");
        }
        SellerRegistration r = timHoacBao(hoSoId);
        batBuocDangXuLy(r);

        String cu = r.getStatus();
        r.setStatus(SellerRegistration.REJECTED);
        r.setReviewNote(lyDo.trim());
        r.setReviewedBy(admin);
        r.setReviewedAt(LocalDateTime.now());
        regRepo.save(r);

        notificationService.gui(r.getApplicant(), Notification.STORE_REJECTED,
                "Hồ sơ " + r.getCode() + " không được duyệt",
                lyDo.trim(),
                "/seller/registration/" + r.getId());

        auditService.ghiDoiTrangThai(admin, AuditLog.REGISTRATION_REJECTED, "SellerRegistration", hoSoId,
                cu, SellerRegistration.REJECTED, lyDo.trim(), ip);
    }

    /**
     * Duyệt hồ sơ — toàn bộ nằm trong MỘT giao dịch (BR-05).
     *
     * Các bước, hỏng bất kỳ bước nào thì cuộn lại hết:
     *   1. Xác định Brand: lấy brand đã chọn, hoặc tạo brand mới từ tên đề xuất
     *   2. Xác định Business: lấy doanh nghiệp đã khai, hoặc tạo từ thông tin hồ sơ
     *   3. Tạo Store ở trạng thái ACTIVE, gắn đúng một Brand (BR-03)
     *   4. Tạo StoreMember OWNER với đủ quyền
     *   5. Cấp vai trò SELLER_OWNER cho người nộp
     *   6. Đóng hồ sơ, trỏ created_store_id về Store vừa tạo
     *   7. Thông báo cho người nộp + ghi nhật ký
     */
    @Transactional
    public Store duyet(Long hoSoId, Long brandIdThayThe, String ghiChu, User admin, String ip) {
        SellerRegistration r = timHoacBao(hoSoId);
        batBuocDangXuLy(r);

        // --- 1. Brand -----------------------------------------------------
        Brand brand;
        if (brandIdThayThe != null) {
            // Admin gộp vào một brand đã có, thường khi phát hiện nghi trùng
            brand = brandRepo.findById(brandIdThayThe)
                    .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy thương hiệu đã chọn."));
        } else if (r.getRequestedBrand() != null) {
            brand = r.getRequestedBrand();
        } else {
            brand = brandService.tao(r.getRequestedBrandName(), null, null, null, admin, true, ip);
        }
        if (brand.isLocked()) {
            throw new LoiNghiepVu("Thương hiệu \"" + brand.getName() + "\" đang bị khoá, không thể duyệt hồ sơ.");
        }
        if (!brand.isActive()) {
            brand.setStatus(Brand.ACTIVE);
            brand.setVerifiedBy(admin);
            brand.setVerifiedAt(LocalDateTime.now());
            brandRepo.save(brand);
        }

        // --- 2. Business --------------------------------------------------
        Business business = r.getBusiness();
        if (business == null) {
            business = new Business();
            business.setLegalName(r.getStoreName());
            business.setAddress(r.getPickupAddress());
            business.setRepresentativeName(r.getApplicant().getFullName());
            business.setRepresentativePhone(r.getContactPhone());
            business.setRepresentativeEmail(r.getContactEmail());
            business.setVerificationStatus(Business.VERIFIED);
            business.setVerifiedBy(admin);
            business.setVerifiedAt(LocalDateTime.now());
            businessRepo.save(business);
            r.setBusiness(business);
        } else if (!business.isVerified()) {
            business.setVerificationStatus(Business.VERIFIED);
            business.setVerifiedBy(admin);
            business.setVerifiedAt(LocalDateTime.now());
            businessRepo.save(business);
        }

        // --- 3. Store -----------------------------------------------------
        Store store = new Store();
        store.setBusiness(business);
        store.setBrand(brand);
        store.setRegistration(r);
        store.setName(r.getStoreName());
        store.setSlug(slugStoreDuyNhat(r.getStoreSlugProposed() != null
                ? r.getStoreSlugProposed()
                : BrandNameNormalizer.slugify(r.getStoreName())));
        store.setStoreType(r.getStoreType());
        store.setPhone(r.getContactPhone());
        store.setEmail(r.getContactEmail());
        store.setPickupAddress(r.getPickupAddress());
        store.setStatus(Store.ACTIVE);
        store.setIsVerified(SellerRegistration.OFFICIAL.equals(r.getStoreType()));
        store.setApprovedAt(LocalDateTime.now());
        store.setApprovedBy(admin);
        storeRepo.save(store);

        // --- 4. Chủ gian hàng ----------------------------------------------
        StoreMember chu = new StoreMember();
        chu.setStore(store);
        chu.setUser(r.getApplicant());
        chu.setMemberRole(StoreMember.OWNER);
        chu.grantAllPermissions();
        chu.setStatus(StoreMember.ACTIVE);
        chu.setInvitedBy(admin);
        memberRepo.save(chu);

        // --- 5. Vai trò -----------------------------------------------------
        userService.themVaiTro(r.getApplicant(), Role.SELLER_OWNER, admin);

        // --- 6. Đóng hồ sơ ---------------------------------------------------
        String trangThaiCu = r.getStatus();
        r.setStatus(SellerRegistration.APPROVED);
        r.setReviewNote(ghiChu);
        r.setReviewedBy(admin);
        r.setReviewedAt(LocalDateTime.now());
        r.setCreatedStore(store);
        regRepo.save(r);

        // --- 7. Thông báo + nhật ký -------------------------------------------
        notificationService.gui(r.getApplicant(), Notification.STORE_APPROVED,
                "Gian hàng \"" + store.getName() + "\" đã được duyệt",
                "Hồ sơ " + r.getCode() + " đã được duyệt. Bạn có thể bắt đầu đăng sản phẩm thương hiệu "
                        + brand.getName() + ".",
                "/seller/" + store.getId() + "/dashboard");

        Map<String, String> ketQua = new LinkedHashMap<>();
        ketQua.put("status", SellerRegistration.APPROVED);
        ketQua.put("storeId", String.valueOf(store.getId()));
        ketQua.put("storeSlug", store.getSlug());
        ketQua.put("brandId", String.valueOf(brand.getId()));
        ketQua.put("brandName", brand.getName());
        auditService.ghi(admin, AuditLog.REGISTRATION_APPROVED, "SellerRegistration", hoSoId,
                Map.of("status", trangThaiCu), ketQua, ghiChu, ip);

        return store;
    }

    /** Đánh dấu một giấy tờ là hợp lệ hay cần nộp lại. */
    @Transactional
    public void danhGiaGiayTo(Long giayToId, String trangThai, String ghiChu) {
        SellerDocument d = docRepo.findById(giayToId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy giấy tờ."));
        if (!List.of(SellerDocument.PENDING, SellerDocument.ACCEPTED, SellerDocument.NEEDS_REPLACEMENT)
                .contains(trangThai)) {
            throw new LoiNghiepVu(Messages.invalid("trạng thái giấy tờ"));
        }
        d.setStatus(trangThai);
        d.setNote(ghiChu);
        docRepo.save(d);
    }

    // =====================================================================
    // Tra cứu
    // =====================================================================

    public SellerRegistration timHoacBao(Long id) {
        return regRepo.findById(id).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy hồ sơ."));
    }

    public SellerRegistration chiTiet(Long id) {
        return regRepo.findDetailById(id).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy hồ sơ."));
    }

    public List<SellerRegistration> cuaNguoiDung(Long userId) {
        return regRepo.findByApplicantIdOrderByCreatedAtDesc(userId);
    }

    public Page<SellerRegistration> danhSach(String tuKhoa, String trangThai, String loai, int trang, int moiTrang) {
        return regRepo.timKiem(
                tuKhoa == null || tuKhoa.isBlank() ? null : tuKhoa.trim(),
                trangThai == null || trangThai.isBlank() ? null : trangThai,
                loai == null || loai.isBlank() ? null : loai,
                PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    public long soChoXuLy() {
        return regRepo.countByStatus(SellerRegistration.PENDING)
                + regRepo.countByStatus(SellerRegistration.UNDER_REVIEW);
    }

    // =====================================================================
    // Nội bộ
    // =====================================================================

    private void batBuocDangXuLy(SellerRegistration r) {
        if (!r.isReviewable()) {
            throw new LoiNghiepVu("Hồ sơ đang ở trạng thái \"" + r.getStatusLabel()
                    + "\" nên không thao tác được nữa.");
        }
    }

    /** Mã hồ sơ dạng HS2026-000123. */
    private String sinhMa() {
        String tienTo = "HS" + Year.now().getValue() + "-";
        long n = regRepo.demTheoTienTo(tienTo) + 1;
        for (int i = 0; i < 50; i++) {
            String ma = tienTo + String.format("%06d", n + i);
            if (!regRepo.existsByCode(ma)) {
                return ma;
            }
        }
        return tienTo + System.currentTimeMillis();
    }

    private String slugStoreDuyNhat(String goc) {
        String s = goc == null || goc.isBlank() ? "gian-hang" : goc;
        if (!storeRepo.existsBySlug(s)) {
            return s;
        }
        for (int i = 2; i < 500; i++) {
            String thu = s + "-" + i;
            if (!storeRepo.existsBySlug(thu)) {
                return thu;
            }
        }
        return s + "-" + System.currentTimeMillis();
    }
}
