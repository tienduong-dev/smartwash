package com.smartwash.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dữ liệu người dùng vừa nhập, giữ lại khi form lỗi (tương đương old() trong Blade) */
public class OldInput implements Serializable {

    private final HashMap<String, String> values = new HashMap<>();
    private final HashMap<String, List<String>> multi = new HashMap<>();

    public OldInput() {
    }

    public OldInput(Map<String, String[]> params) {
        params.forEach((k, v) -> {
            if (k.startsWith("_") || k.contains("password") || v == null) {
                return;
            }
            String key = k.endsWith("[]") ? k.substring(0, k.length() - 2) : k;
            values.put(key, v.length > 0 ? v[0] : "");
            multi.put(key, List.of(v));
        });
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public String get(String key) {
        return values.get(key);
    }

    /** old('key', default) */
    public Object get(String key, Object defaultValue) {
        return values.containsKey(key) ? values.get(key) : defaultValue;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<String> list(String key) {
        return multi.getOrDefault(key, List.of());
    }
}
