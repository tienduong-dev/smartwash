package com.smartwash.common;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

/** Đọc dữ liệu gửi lên: hỗ trợ cả form thường lẫn JSON (fetch) — giống $request->input() */
public final class Body {

    private static final String CACHE = "parsed_body";

    private Body() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> read(HttpServletRequest request) {
        Object cached = request.getAttribute(CACHE);
        if (cached instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        Map<String, Object> data = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> data.put(k, v.length == 1 ? v[0] : v));
        String type = request.getContentType();
        if (type != null && type.contains("json")) {
            try {
                String raw = new String(request.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                JsonNode node = JsonUtil.read(raw);
                if (node != null && node.isObject()) {
                    node.fields().forEachRemaining(f -> {
                        JsonNode v = f.getValue();
                        data.put(f.getKey(), v.isNull() ? null : v.isValueNode() ? v.asText() : v.toString());
                    });
                }
            } catch (Exception ignored) {
                // body rỗng
            }
        }
        request.setAttribute(CACHE, data);
        return data;
    }

    public static String str(HttpServletRequest request, String key) {
        Object v = read(request).get(key);
        return v == null ? null : v.toString();
    }

    public static boolean has(HttpServletRequest request, String key) {
        return read(request).containsKey(key);
    }
}
