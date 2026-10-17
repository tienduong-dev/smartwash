package com.smartwash.common;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Hàm định dạng dùng chung (tương đương number_format / format ngày của PHP).
 * Trong Thymeleaf gọi bằng: ${T(com.smartwash.common.Fmt).vnd(x)}
 * hoặc qua bean @fmt: ${@fmt.vnd(x)}
 */
public final class Fmt {

    private Fmt() {
    }

    /** number_format($n, 0, ',', '.') */
    public static String number(Number n) {
        if (n == null) {
            n = 0;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        df.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return df.format(new BigDecimal(n.toString()));
    }

    /** number_format($n, decimals, ',', '.') */
    public static String number(Number n, int decimals) {
        if (n == null) {
            n = 0;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        String pattern = decimals > 0 ? "#,##0." + "0".repeat(decimals) : "#,##0";
        DecimalFormat df = new DecimalFormat(pattern, symbols);
        df.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return df.format(new BigDecimal(n.toString()));
    }

    /** Dạng "1.234.000 đ" (có dấu cách) */
    public static String vnd(Number n) {
        return number(n) + " đ";
    }

    /** Dạng "1.234.000đ" (không dấu cách) */
    public static String vndShort(Number n) {
        return number(n) + "đ";
    }

    /** number_format($n, 1) kiểu PHP mặc định: "4.5" */
    public static String oneDecimal(Number n) {
        if (n == null) {
            n = 0;
        }
        return String.format(Locale.US, "%.1f", n.doubleValue());
    }

    public static String date(LocalDateTime dt, String pattern) {
        if (dt == null) {
            return "";
        }
        return dt.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String upperFirst(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.substring(0, s.offsetByCodePoints(0, 1)).toUpperCase(new Locale("vi"));
    }
}
