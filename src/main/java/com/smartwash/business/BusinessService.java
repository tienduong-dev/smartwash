package com.smartwash.business;

import com.smartwash.audit.AuditService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Messages;
import com.smartwash.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/** Doanh nghiệp đứng sau gian hàng (đặc tả mục 4.2, BR-02). */
@Service
public class BusinessService {

    private final BusinessRepository repo;
    private final AuditService auditService;

    public BusinessService(BusinessRepository repo, AuditService auditService) {
        this.repo = repo;
        this.auditService = auditService;
    }

    @Transactional
    public Business tao(String tenPhapNhan, String maSoThue, String diaChi,
                        String nguoiDaiDien, String dienThoai, String email) {
        if (tenPhapNhan == null || tenPhapNhan.trim().isEmpty()) {
            throw new LoiNghiepVu("legalName", Messages.required("tên pháp nhân"));
        }
        String mst = maSoThue == null || maSoThue.isBlank() ? null : maSoThue.trim();
        if (mst != null && repo.existsByTaxCode(mst)) {
            throw new LoiNghiepVu("taxCode", "Mã số thuế này đã được đăng ký.");
        }

        Business b = new Business();
        b.setLegalName(tenPhapNhan.trim());
        b.setTaxCode(mst);
        b.setAddress(diaChi);
        b.setRepresentativeName(nguoiDaiDien);
        b.setRepresentativePhone(dienThoai);
        b.setRepresentativeEmail(email);
        b.setVerificationStatus(Business.PENDING);
        return repo.save(b);
    }

    @Transactional
    public void xacMinh(Long id, User admin, String ip) {
        Business b = timHoacBao(id);
        String cu = b.getVerificationStatus();
        b.setVerificationStatus(Business.VERIFIED);
        b.setVerifiedBy(admin);
        b.setVerifiedAt(LocalDateTime.now());
        repo.save(b);
        auditService.ghiDoiTrangThai(admin, "BUSINESS_VERIFIED", "Business", id, cu, Business.VERIFIED, null, ip);
    }

    @Transactional
    public void tuChoi(Long id, String lyDo, User admin, String ip) {
        Business b = timHoacBao(id);
        String cu = b.getVerificationStatus();
        b.setVerificationStatus(Business.REJECTED);
        b.setNote(lyDo);
        b.setVerifiedBy(admin);
        b.setVerifiedAt(LocalDateTime.now());
        repo.save(b);
        auditService.ghi(admin, "BUSINESS_REJECTED", "Business", id,
                Map.of("status", cu), Map.of("status", Business.REJECTED), lyDo, ip);
    }

    public Business timHoacBao(Long id) {
        return repo.findById(id).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy doanh nghiệp."));
    }

    public Page<Business> danhSach(String tuKhoa, String trangThai, int trang, int moiTrang) {
        return repo.timKiem(
                tuKhoa == null || tuKhoa.isBlank() ? null : tuKhoa.trim(),
                trangThai == null || trangThai.isBlank() ? null : trangThai,
                PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }
}
