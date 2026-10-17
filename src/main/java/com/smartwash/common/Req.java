package com.smartwash.common;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;

/** Hàm tiện ích đọc dữ liệu request (giống $request->filled(), boolean(), ...) */
public final class Req {

    private Req() {
    }

    public static boolean wantsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String xrw = request.getHeader("X-Requested-With");
        return (accept != null && accept.contains("json")) || "XMLHttpRequest".equals(xrw);
    }

    /** redirect()->back(): quay lại trang trước (theo Referer), nếu không có thì về fallback */
    public static String back(HttpServletRequest request, String fallback) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                java.net.URI uri = java.net.URI.create(referer);
                if (uri.getHost() == null || uri.getHost().equals(request.getServerName())) {
                    String path = uri.getRawPath() == null ? "/" : uri.getRawPath();
                    return "redirect:" + path + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
                }
            } catch (Exception ignored) {
                // Referer lỗi -> dùng fallback
            }
        }
        return "redirect:" + fallback;
    }

    public static boolean filled(String s) {
        return s != null && !s.trim().isEmpty();
    }

    public static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public static String nullIfBlank(String s) {
        return filled(s) ? s.trim() : null;
    }

    /** $request->boolean() */
    public static boolean bool(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        String s = v.toString().trim().toLowerCase();
        return s.equals("1") || s.equals("true") || s.equals("on") || s.equals("yes");
    }

    public static Long toLong(String s) {
        try {
            return filled(s) ? Long.parseLong(s.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer toInt(String s) {
        try {
            return filled(s) ? Integer.parseInt(s.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal toDecimal(String s) {
        try {
            return filled(s) ? new BigDecimal(s.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isNumeric(String s) {
        return toDecimal(s) != null;
    }

    public static boolean isEmail(String s) {
        return s != null && s.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
