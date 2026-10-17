package com.smartwash.common;

/**
 * Lỗi nghiệp vụ có thể hiển thị thẳng cho người dùng.
 *
 * Khác với lỗi kỹ thuật: thông điệp đã viết bằng tiếng Việt, không lộ chi tiết
 * nội bộ, và controller chỉ cần bắt rồi đưa vào flash message.
 */
public class LoiNghiepVu extends RuntimeException {

    private final String truong;

    public LoiNghiepVu(String thongDiep) {
        this(null, thongDiep);
    }

    /** @param truong tên trường form gây lỗi, để tô đỏ đúng ô; null nếu lỗi chung */
    public LoiNghiepVu(String truong, String thongDiep) {
        super(thongDiep);
        this.truong = truong;
    }

    public String getTruong() {
        return truong;
    }

    public boolean coTruong() {
        return truong != null;
    }
}
