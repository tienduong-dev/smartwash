package com.smartwash.brand;

import com.smartwash.audit.AuditLog;
import com.smartwash.audit.AuditService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Messages;
import com.smartwash.store.StoreRepository;
import com.smartwash.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Thương hiệu (đặc tả mục 4.3, 9.3, BR-02).
 *
 * Quy tắc quan trọng: KHÔNG tự động chặn tên trùng. Hệ thống chỉ phát hiện
 * nghi vấn qua normalized_name rồi để Admin quyết định, vì "Aqua" của hãng
 * Nhật và "Aqua" của một nhà lắp ráp trong nước có thể là hai thương hiệu
 * khác nhau thật.
 */
@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final StoreRepository storeRepository;
    private final AuditService auditService;

    public BrandService(BrandRepository brandRepository,
                        StoreRepository storeRepository,
                        AuditService auditService) {
        this.brandRepository = brandRepository;
        this.storeRepository = storeRepository;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------- tạo / sửa

    /**
     * Tạo thương hiệu. Trạng thái ban đầu tuỳ người tạo: Admin tạo thì
     * ACTIVE ngay, người bán đề xuất thì PENDING_VERIFICATION.
     */
    @Transactional
    public Brand tao(String ten, String moTa, String website, String quocGia,
                     User nguoiTao, boolean kichHoatNgay, String ip) {
        if (ten == null || ten.trim().isEmpty()) {
            throw new LoiNghiepVu("name", Messages.required("tên thương hiệu"));
        }

        Brand b = new Brand();
        b.setNameAndNormalize(ten.trim());
        b.setSlug(slugDuyNhat(BrandNameNormalizer.slugify(ten)));
        b.setDescription(moTa);
        b.setWebsite(website);
        b.setCountry(quocGia);
        b.setCreatedBy(nguoiTao);
        b.setStatus(kichHoatNgay ? Brand.ACTIVE : Brand.PENDING_VERIFICATION);
        if (kichHoatNgay) {
            b.setVerifiedBy(nguoiTao);
            b.setVerifiedAt(LocalDateTime.now());
        }
        brandRepository.save(b);

        auditService.ghi(nguoiTao, "BRAND_CREATED", "Brand", b.getId(), null,
                Map.of("name", b.getName(), "status", b.getStatus()), null, ip);
        return b;
    }

    @Transactional
    public Brand capNhat(Long id, String ten, String moTa, String website, String quocGia,
                         String logo, User nguoiSua, String ip) {
        Brand b = timHoacBao(id);
        Map<String, String> cu = Map.of("name", b.getName(), "website", b.getWebsite() == null ? "" : b.getWebsite());

        if (ten != null && !ten.trim().isEmpty() && !ten.trim().equals(b.getName())) {
            b.setNameAndNormalize(ten.trim());
        }
        b.setDescription(moTa);
        b.setWebsite(website);
        b.setCountry(quocGia);
        if (logo != null) {
            b.setLogo(logo);
        }
        brandRepository.save(b);

        auditService.ghi(nguoiSua, "BRAND_UPDATED", "Brand", id, cu,
                Map.of("name", b.getName(), "website", b.getWebsite() == null ? "" : b.getWebsite()), null, ip);
        return b;
    }

    // ---------------------------------------------------------- duyệt / khoá

    @Transactional
    public void xacMinh(Long id, User admin, String ip) {
        Brand b = timHoacBao(id);
        String cu = b.getStatus();
        b.setStatus(Brand.ACTIVE);
        b.setVerifiedBy(admin);
        b.setVerifiedAt(LocalDateTime.now());
        brandRepository.save(b);
        auditService.ghiDoiTrangThai(admin, AuditLog.BRAND_VERIFIED, "Brand", id, cu, Brand.ACTIVE, null, ip);
    }

    /**
     * Khoá thương hiệu. Các gian hàng đang gắn brand này sẽ không bán được
     * nữa (Store.canSell kiểm tra brand.isActive), nhưng dữ liệu vẫn giữ
     * nguyên để tra cứu đơn cũ.
     */
    @Transactional
    public void khoa(Long id, String lyDo, User admin, String ip) {
        Brand b = timHoacBao(id);
        long soStore = storeRepository.countByBrandId(id);
        String cu = b.getStatus();
        b.setStatus(Brand.LOCKED);
        brandRepository.save(b);
        auditService.ghi(admin, AuditLog.BRAND_LOCKED, "Brand", id,
                Map.of("status", cu),
                Map.of("status", Brand.LOCKED, "affectedStores", String.valueOf(soStore)),
                lyDo, ip);
    }

    @Transactional
    public void moKhoa(Long id, User admin, String ip) {
        Brand b = timHoacBao(id);
        String cu = b.getStatus();
        b.setStatus(Brand.ACTIVE);
        brandRepository.save(b);
        auditService.ghiDoiTrangThai(admin, "BRAND_UNLOCKED", "Brand", id, cu, Brand.ACTIVE, null, ip);
    }

    // ------------------------------------------------------------- tra cứu

    public Brand timHoacBao(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy thương hiệu."));
    }

    /** BR-02: danh sách thương hiệu nghi trùng với tên đang nhập. */
    public List<Brand> nghiTrung(String ten, Long boQuaId) {
        String khoa = BrandNameNormalizer.normalize(ten);
        if (khoa.isEmpty()) {
            return List.of();
        }
        return brandRepository.nghiTrung(khoa, boQuaId);
    }

    public List<Brand> choChonHoSo() {
        return brandRepository.choChonHoSo();
    }

    public Page<Brand> danhSach(String tuKhoa, String trangThai, int trang, int moiTrang) {
        return brandRepository.timKiem(
                tuKhoa == null || tuKhoa.isBlank() ? null : tuKhoa.trim(),
                trangThai == null || trangThai.isBlank() ? null : trangThai,
                PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    /** Sinh slug chưa bị dùng; thêm hậu tố -2, -3... khi trùng. */
    public String slugDuyNhat(String goc) {
        String s = goc == null || goc.isBlank() ? "thuong-hieu" : goc;
        if (!brandRepository.existsBySlug(s)) {
            return s;
        }
        for (int i = 2; i < 500; i++) {
            String thu = s + "-" + i;
            if (!brandRepository.existsBySlug(thu)) {
                return thu;
            }
        }
        return s + "-" + System.currentTimeMillis();
    }
}
