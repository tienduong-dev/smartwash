package com.smartwash.common;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Collection;
import java.util.zip.CRC32;

/**
 * Hàm tiện ích gọi trực tiếp trong giao diện Thymeleaf: ${@fmt.vnd(product.price)}
 */
@Component("fmt")
public class ViewHelper {

    public String vnd(Number n) {
        return Fmt.vnd(n);
    }

    public String vndShort(Number n) {
        return Fmt.vndShort(n);
    }

    public String number(Number n) {
        return Fmt.number(n);
    }

    public String number(Number n, int decimals) {
        return Fmt.number(n, decimals);
    }

    /** number_format($n, 1) mặc định của PHP: dấu chấm thập phân, dấu phẩy hàng nghìn */
    public String decimal1(Number n) {
        if (n == null) {
            n = 0;
        }
        return String.format(java.util.Locale.US, "%,.1f", n.doubleValue());
    }

    public String date(LocalDateTime dt, String pattern) {
        return Fmt.date(dt, pattern);
    }

    /** Giá trị cho ô &lt;input type="datetime-local"&gt;: 2026-10-08T14:30 */
    public String dtLocal(LocalDateTime dt) {
        return dt == null ? "" : dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    public int year() {
        return Year.now().getValue();
    }

    public String json(Object o) {
        return JsonUtil.write(o);
    }

    public boolean has(Collection<?> list, Object value) {
        if (list == null || value == null) {
            return false;
        }
        for (Object o : list) {
            if (o != null && o.toString().equals(value.toString())) {
                return true;
            }
        }
        return false;
    }

    /** strtoupper(substr($s, 0, 1)) */
    public String initial(String s) {
        return Fmt.upperFirst(s);
    }

    /** Str::limit($s, n) */
    public String limit(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.codePointCount(0, s.length()) <= n ? s : s.substring(0, s.offsetByCodePoints(0, n)) + "...";
    }

    public long crc32Mod(String s, int mod) {
        CRC32 crc = new CRC32();
        crc.update((s == null ? "" : s).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return crc.getValue() % mod;
    }

    /** rtrim(rtrim($capacity, '0'), '.') : 9.0 -> "9", 10.5 -> "10.5" */
    public String trimZero(Number n) {
        if (n == null) {
            return "";
        }
        return new java.math.BigDecimal(n.toString()).stripTrailingZeros().toPlainString();
    }

    /** Số thập phân không có đuôi 0 thừa: 10.50 -> "10.5", 12.00 -> "12" */
    public String plain(java.math.BigDecimal b) {
        if (b == null) {
            return "";
        }
        java.math.BigDecimal x = b.stripTrailingZeros();
        return (x.scale() < 0 ? x.setScale(0) : x).toPlainString();
    }

    public boolean startsWith(String s, String prefix) {
        return s != null && s.startsWith(prefix);
    }

    public long round(Number n) {
        return n == null ? 0 : Math.round(n.doubleValue());
    }
}
