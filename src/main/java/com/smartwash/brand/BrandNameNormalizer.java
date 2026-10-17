package com.smartwash.brand;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Chuẩn hoá tên thương hiệu để dò trùng (BR-02, đặc tả mục 9.3).
 *
 * Mục tiêu: "Electrolux", "ELECTROLUX ", "Electrolux Việt Nam" và
 * "Công ty TNHH Electrolux" đều ra cùng một khoá, để Admin thấy nghi vấn
 * trùng và tự quyết định gộp hay tách. Hàm này KHÔNG tự gộp gì cả.
 */
public final class BrandNameNormalizer {

    private BrandNameNormalizer() {
    }

    /** Các đuôi pháp nhân và hậu tố vùng miền cần bỏ khi so khớp. */
    private static final List<String> BO_QUA = List.of(
            "CONG TY TNHH MOT THANH VIEN",
            "CONG TY TNHH MTV",
            "CONG TY CO PHAN",
            "CONG TY TNHH",
            "CONG TY",
            "TNHH MTV",
            "TNHH",
            "CO PHAN",
            "DOANH NGHIEP TU NHAN",
            "CO LTD",
            "CO., LTD",
            "LTD",
            "LLC",
            "JSC",
            "INC",
            "CORP",
            "CORPORATION",
            "COMPANY",
            "GROUP",
            "HOLDINGS",
            "VIET NAM",
            "VIETNAM",
            "VN"
    );

    /**
     * Khoá chuẩn hoá: bỏ dấu, viết hoa, bỏ ký tự không phải chữ/số,
     * bỏ đuôi pháp nhân, gộp khoảng trắng.
     *
     * @return chuỗi rỗng nếu đầu vào rỗng — việc chặn tên rỗng là của tầng
     *         kiểm tra dữ liệu, không phải của hàm này.
     */
    public static String normalize(String ten) {
        if (ten == null) {
            return "";
        }
        // 1. bỏ dấu tiếng Việt
        String s = Normalizer.normalize(ten, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('Đ', 'D')
                .replace('đ', 'd');

        // 2. viết hoa, đổi mọi ký tự lạ thành khoảng trắng
        s = s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ").trim();

        // 3. bỏ đuôi pháp nhân, lặp vì có thể chồng nhau
        //    ("CONG TY TNHH ELECTROLUX VIET NAM" -> "ELECTROLUX")
        boolean doi = true;
        while (doi && !s.isEmpty()) {
            doi = false;
            for (String tu : BO_QUA) {
                if (s.startsWith(tu + " ")) {
                    s = s.substring(tu.length()).trim();
                    doi = true;
                } else if (s.endsWith(" " + tu)) {
                    s = s.substring(0, s.length() - tu.length()).trim();
                    doi = true;
                } else if (s.equals(tu)) {
                    // tên chỉ còn đuôi pháp nhân: giữ nguyên, không xoá sạch
                    return s;
                }
            }
        }

        return s.replaceAll("\\s+", " ").trim();
    }

    /** Sinh slug dạng url từ tên thương hiệu. */
    public static String slugify(String ten) {
        if (ten == null) {
            return "";
        }
        String s = Normalizer.normalize(ten, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('Đ', 'D')
                .replace('đ', 'd')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return s;
    }

    /** Hai tên có bị nghi là cùng một thương hiệu không. */
    public static boolean trungKhoa(String a, String b) {
        String na = normalize(a);
        return !na.isEmpty() && na.equals(normalize(b));
    }
}
