package com.smartwash.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lưu file tải lên.
 *
 * Hai khu riêng biệt:
 *   - công khai (uploads/)        : ảnh sản phẩm, logo — phục vụ qua /storage/**
 *   - riêng tư  (uploads-private/): giấy tờ pháp lý — KHÔNG phục vụ tĩnh, phải
 *                                   đi qua controller có kiểm tra quyền (mục 12.1)
 */
@Service
public class LuuTru {

    private static final List<String> ANH_CHO_PHEP =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final List<String> TAI_LIEU_CHO_PHEP =
            List.of("image/jpeg", "image/png", "image/webp", "application/pdf");

    private static final long TOI_DA_ANH = 5L * 1024 * 1024;       // 5 MB
    private static final long TOI_DA_TAI_LIEU = 10L * 1024 * 1024; // 10 MB

    private final Path goiCongKhai;
    private final Path goiRiengTu;

    public LuuTru(@Value("${app.upload-dir}") String thuMucCongKhai,
                  @Value("${app.private-upload-dir:uploads-private}") String thuMucRiengTu) {
        this.goiCongKhai = Paths.get(thuMucCongKhai).toAbsolutePath().normalize();
        this.goiRiengTu = Paths.get(thuMucRiengTu).toAbsolutePath().normalize();
    }

    // ------------------------------------------------------------ công khai

    /**
     * Lưu ảnh công khai.
     *
     * @param nhom thư mục con, ví dụ "products", "brands", "stores"
     * @return đường dẫn tương đối để ghi vào CSDL, ví dụ "products/2026/10/abc.webp"
     */
    public String luuAnh(MultipartFile file, String nhom) {
        kiemTra(file, ANH_CHO_PHEP, TOI_DA_ANH, "Ảnh");
        return ghi(file, goiCongKhai, nhom);
    }

    // ------------------------------------------------------------- riêng tư

    /** Lưu giấy tờ pháp lý vào khu riêng tư. */
    public String luuTaiLieu(MultipartFile file, String nhom) {
        kiemTra(file, TAI_LIEU_CHO_PHEP, TOI_DA_TAI_LIEU, "Tài liệu");
        return ghi(file, goiRiengTu, nhom);
    }

    /**
     * Mở file riêng tư để tải về.
     *
     * Chặn vượt thư mục: đường dẫn sau khi chuẩn hoá phải vẫn nằm trong gốc.
     */
    public InputStream docRiengTu(String duongDanTuongDoi) {
        Path f = goiRiengTu.resolve(duongDanTuongDoi).normalize();
        if (!f.startsWith(goiRiengTu)) {
            throw new LoiNghiepVu("Đường dẫn tệp không hợp lệ.");
        }
        if (!Files.isRegularFile(f)) {
            throw new LoiNghiepVu("Không tìm thấy tệp.");
        }
        try {
            return Files.newInputStream(f);
        } catch (IOException e) {
            throw new LoiNghiepVu("Không đọc được tệp.");
        }
    }

    public long kichThuocRiengTu(String duongDanTuongDoi) {
        Path f = goiRiengTu.resolve(duongDanTuongDoi).normalize();
        try {
            return f.startsWith(goiRiengTu) && Files.isRegularFile(f) ? Files.size(f) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    public void xoaCongKhai(String duongDanTuongDoi) {
        if (duongDanTuongDoi == null || duongDanTuongDoi.isBlank()) {
            return;
        }
        Path f = goiCongKhai.resolve(duongDanTuongDoi).normalize();
        if (f.startsWith(goiCongKhai)) {
            try {
                Files.deleteIfExists(f);
            } catch (IOException ignored) {
                // Xoá hụt một file rác không đáng để hỏng cả thao tác của người dùng
            }
        }
    }

    // ------------------------------------------------------------- nội bộ

    private void kiemTra(MultipartFile file, List<String> loaiChoPhep, long toiDa, String nhan) {
        if (file == null || file.isEmpty()) {
            throw new LoiNghiepVu(nhan + " không được để trống.");
        }
        if (file.getSize() > toiDa) {
            throw new LoiNghiepVu(nhan + " không được quá " + (toiDa / 1024 / 1024) + " MB.");
        }
        String mime = file.getContentType();
        if (mime == null || !loaiChoPhep.contains(mime.toLowerCase(Locale.ROOT))) {
            throw new LoiNghiepVu(nhan + " phải có định dạng: "
                    + String.join(", ", loaiChoPhep).replace("image/", "").replace("application/", ""));
        }
    }

    private String ghi(MultipartFile file, Path goc, String nhom) {
        LocalDate hom_nay = LocalDate.now();
        String thuMucCon = String.format("%s/%d/%02d", lamSach(nhom), hom_nay.getYear(), hom_nay.getMonthValue());
        String ten = UUID.randomUUID().toString().replace("-", "") + duoiFile(file.getOriginalFilename());

        try {
            Path dich = goc.resolve(thuMucCon).normalize();
            if (!dich.startsWith(goc)) {
                throw new LoiNghiepVu("Thư mục lưu không hợp lệ.");
            }
            Files.createDirectories(dich);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dich.resolve(ten), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new LoiNghiepVu("Không lưu được tệp, vui lòng thử lại.");
        }
        return thuMucCon + "/" + ten;
    }

    private String lamSach(String s) {
        return s == null ? "khac" : s.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private String duoiFile(String tenGoc) {
        if (tenGoc == null) {
            return "";
        }
        int cham = tenGoc.lastIndexOf('.');
        if (cham < 0 || cham == tenGoc.length() - 1) {
            return "";
        }
        String duoi = tenGoc.substring(cham).toLowerCase(Locale.ROOT);
        return duoi.matches("^\\.[a-z0-9]{1,5}$") ? duoi : "";
    }
}
