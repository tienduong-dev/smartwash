package com.smartwash.common;

/** Câu thông báo kiểm tra dữ liệu, tiếng Việt. */
public final class Messages {

    private Messages() {
    }

    public static String required(String nhan) {
        return "Vui lòng nhập " + nhan + ".";
    }

    public static String chon(String nhan) {
        return "Vui lòng chọn " + nhan + ".";
    }

    public static String email(String nhan) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1) + " không đúng định dạng.";
    }

    public static String min(String nhan, int n) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1) + " phải có ít nhất " + n + " ký tự.";
    }

    public static String max(String nhan, int n) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1) + " không được quá " + n + " ký tự.";
    }

    public static String confirmed(String nhan) {
        return "Xác nhận " + nhan + " không khớp.";
    }

    public static String unique(String nhan) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1) + " này đã được sử dụng.";
    }

    public static String numeric(String nhan) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1) + " phải là số.";
    }

    public static String minValue(String nhan, long n) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1) + " phải từ " + n + " trở lên.";
    }

    public static String maxValue(String nhan, long n) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1) + " không được quá " + n + ".";
    }

    public static String invalid(String nhan) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1) + " không hợp lệ.";
    }

    public static String phone(String nhan) {
        return nhan.substring(0, 1).toUpperCase() + nhan.substring(1)
                + " phải là số điện thoại Việt Nam hợp lệ (10 chữ số, bắt đầu bằng 0).";
    }
}
