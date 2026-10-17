package com.smartwash.config;

import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Req;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Xử lý lỗi tập trung.
 *
 * Lỗi nghiệp vụ quay lại trang trước kèm thông báo, không đổ stack trace ra
 * màn hình cho người dùng xem.
 */
@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(LoiNghiepVu.class)
    public Object loiNghiepVu(LoiNghiepVu e, HttpServletRequest request, RedirectAttributes ra) {
        if (Req.wantsJson(request)) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        ra.addFlashAttribute("error", e.getMessage());
        return Req.back(request, "/");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object quaDungLuong(HttpServletRequest request, RedirectAttributes ra) {
        String thongDiep = "Tệp tải lên quá dung lượng cho phép.";
        if (Req.wantsJson(request)) {
            return ResponseEntity.badRequest().body(Map.of("message", thongDiep));
        }
        ra.addFlashAttribute("error", thongDiep);
        return Req.back(request, "/");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String khongTimThay(HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return "error/404";
    }
}
