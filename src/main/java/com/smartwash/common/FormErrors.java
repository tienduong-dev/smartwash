package com.smartwash.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Danh sách lỗi validate của form (tương đương $errors trong Blade) */
public class FormErrors implements Serializable {

    private final Map<String, String> messages = new LinkedHashMap<>();

    public void add(String field, String message) {
        messages.putIfAbsent(field, message);
    }

    public boolean has(String field) {
        return messages.containsKey(field);
    }

    public String first(String field) {
        return messages.get(field);
    }

    public boolean any() {
        return !messages.isEmpty();
    }

    public List<String> all() {
        return new ArrayList<>(messages.values());
    }

    public String firstMessage() {
        return messages.isEmpty() ? null : messages.values().iterator().next();
    }
}
