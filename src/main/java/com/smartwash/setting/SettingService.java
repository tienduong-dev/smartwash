package com.smartwash.setting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cấu hình toàn sàn.
 *
 * Giữ một bản nhớ tạm trong bộ nhớ vì các giá trị này được đọc ở gần như mọi
 * yêu cầu (tỷ lệ hoa hồng, hạn đổi trả) nhưng rất ít khi đổi.
 */
@Service
public class SettingService {

    private final SettingRepository repo;
    private final Map<String, String> nhoTam = new LinkedHashMap<>();
    private volatile boolean daNap = false;

    public SettingService(SettingRepository repo) {
        this.repo = repo;
    }

    private synchronized void nap() {
        if (daNap) {
            return;
        }
        nhoTam.clear();
        for (Setting s : repo.findAll()) {
            nhoTam.put(s.getKey(), s.getValue());
        }
        daNap = true;
    }

    public String chuoi(String khoa, String macDinh) {
        nap();
        String v = nhoTam.get(khoa);
        return v == null || v.isBlank() ? macDinh : v;
    }

    public int so(String khoa, int macDinh) {
        try {
            return Integer.parseInt(chuoi(khoa, String.valueOf(macDinh)).trim());
        } catch (NumberFormatException e) {
            return macDinh;
        }
    }

    public BigDecimal thapPhan(String khoa, BigDecimal macDinh) {
        try {
            return new BigDecimal(chuoi(khoa, macDinh.toPlainString()).trim());
        } catch (NumberFormatException e) {
            return macDinh;
        }
    }

    public boolean co(String khoa, boolean macDinh) {
        String v = chuoi(khoa, macDinh ? "1" : "0").trim().toLowerCase();
        return v.equals("1") || v.equals("true") || v.equals("yes");
    }

    // ------------------------------------------------------- giá trị hay dùng

    /** Tỷ lệ phí sàn mặc định, phần trăm. */
    public BigDecimal tyLeHoaHong() {
        return thapPhan(Setting.COMMISSION_RATE, new BigDecimal("5.00"));
    }

    /** Số ngày được gửi yêu cầu đổi trả sau khi giao thành công. */
    public int soNgayDoiTra() {
        return so(Setting.RETURN_WINDOW_DAYS, 7);
    }

    public boolean batBuocDuyetSanPham() {
        return co(Setting.PRODUCT_REVIEW_REQUIRED, true);
    }

    @Transactional
    public void dat(String khoa, String giaTri, String moTa) {
        Setting s = repo.findByKey(khoa).orElseGet(() -> {
            Setting moi = new Setting();
            moi.setKey(khoa);
            return moi;
        });
        s.setValue(giaTri);
        if (moTa != null) {
            s.setDescription(moTa);
        }
        repo.save(s);
        daNap = false;
    }

    /** Buộc nạp lại khi có nơi khác sửa trực tiếp bảng settings. */
    public void lamMoi() {
        daNap = false;
    }
}
