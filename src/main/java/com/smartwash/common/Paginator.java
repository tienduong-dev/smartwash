package com.smartwash.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Thông tin phân trang cho view (tương đương LengthAwarePaginator + ->withQueryString() của Laravel).
 * Trang bắt đầu từ 1, tham số ?page=
 */
public class Paginator<T> {

    private final Page<T> page;
    private final String baseUrl;

    public Paginator(Page<T> page, HttpServletRequest request) {
        this.page = page;
        this.baseUrl = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
    }

    /** Đọc ?page= (>=1) thành chỉ số trang của Spring (bắt đầu từ 0) */
    public static int pageIndex(String pageParam) {
        try {
            return Math.max(Integer.parseInt(pageParam) - 1, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public List<T> getItems() { return page.getContent(); }
    public boolean isEmpty() { return page.isEmpty(); }
    public boolean isNotEmpty() { return !page.isEmpty(); }
    public long getTotal() { return page.getTotalElements(); }
    public int getCurrentPage() { return page.getNumber() + 1; }
    public int getLastPage() { return Math.max(page.getTotalPages(), 1); }
    public int getPerPage() { return page.getSize(); }
    public boolean hasPages() { return page.getTotalPages() > 1; }
    public boolean onFirstPage() { return page.getNumber() == 0; }
    public boolean hasMorePages() { return page.hasNext(); }

    public Long getFirstItem() {
        return page.isEmpty() ? null : (long) page.getNumber() * page.getSize() + 1;
    }

    public Long getLastItem() {
        return page.isEmpty() ? null : getFirstItem() + page.getNumberOfElements() - 1;
    }

    public String url(int p) {
        return UriComponentsBuilder.fromUriString(baseUrl).replaceQueryParam("page", p).build().toUriString();
    }

    public String getPreviousPageUrl() { return onFirstPage() ? null : url(getCurrentPage() - 1); }
    public String getNextPageUrl() { return hasMorePages() ? url(getCurrentPage() + 1) : null; }

    /** Danh sách số trang, 0 = dấu "..." (giống UrlWindow của Laravel, onEachSide = 3) */
    public List<Integer> getElements() {
        int last = getLastPage();
        int current = getCurrentPage();
        int onEachSide = 3;
        int window = onEachSide * 2;
        List<Integer> result = new ArrayList<>();
        if (last < window + 8) {
            for (int i = 1; i <= last; i++) result.add(i);
            return result;
        }
        if (current <= window) {
            for (int i = 1; i <= window + 2; i++) result.add(i);
            result.add(0);
            result.add(last - 1);
            result.add(last);
        } else if (current > last - window) {
            result.add(1);
            result.add(2);
            result.add(0);
            for (int i = last - (window + 2); i <= last; i++) result.add(i);
        } else {
            result.add(1);
            result.add(2);
            result.add(0);
            for (int i = current - onEachSide; i <= current + onEachSide; i++) result.add(i);
            result.add(0);
            result.add(last - 1);
            result.add(last);
        }
        return result;
    }
}
