package com.smartwash.admin;

import com.smartwash.audit.AuditLogRepository;
import com.smartwash.common.Paginator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/** Xem nhật ký thao tác (đặc tả mục 12.3, BR-15). */
@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    private final AuditLogRepository repo;

    public AdminAuditController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String danhSach(HttpServletRequest request,
                           @RequestParam(required = false) String action,
                           @RequestParam(required = false) String entityType,
                           @RequestParam(required = false) Long actorId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        model.addAttribute("ds", new Paginator<>(repo.timKiem(
                rong(action), rong(entityType), actorId,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(0, page - 1), 40)), request));
        model.addAttribute("cacHanhDong", repo.cacHanhDong());
        model.addAttribute("cacLoai", repo.cacLoaiDoiTuong());
        model.addAttribute("action", action);
        model.addAttribute("entityType", entityType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/nhat-ky";
    }

    private String rong(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
