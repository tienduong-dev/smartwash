package com.smartwash.config;

import com.smartwash.auth.AuthService;
import com.smartwash.common.Req;
import com.smartwash.store.StoreContext;
import com.smartwash.store.StoreMember;
import com.smartwash.store.StoreMemberRepository;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Optional;

/**
 * Chặn truy cập theo nhóm đường dẫn (đặc tả mục 12.2).
 *
 *  /admin/**             : phải đăng nhập + vai trò ADMIN
 *  /seller/register, /seller/registration/** : chỉ cần đăng nhập
 *  /seller/{id}/**       : phải là thành viên đang làm việc của gian hàng đó
 *  nhóm cần đăng nhập    : /account, /orders, /checkout, /addresses, /notifications...
 *  nhóm mua sắm          : tài khoản quản trị không dùng
 *
 * Kiểm tra quyền chi tiết trong gian hàng (sửa sản phẩm, xử lý đơn) nằm ở
 * controller qua StoreContext, vì nó phụ thuộc từng chức năng.
 */
@Component
public class AccessInterceptor implements HandlerInterceptor {

    /** Tư cách thành viên đã tra ở interceptor, controller dùng lại khỏi truy vấn hai lần. */
    public static final String STORE_MEMBER = "__storeMember";

    private final AuthService authService;
    private final StoreMemberRepository memberRepo;

    public AccessInterceptor(AuthService authService, StoreMemberRepository memberRepo) {
        this.authService = authService;
        this.memberRepo = memberRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        User user = authService.currentUser(request);

        // ---------------------------------------------------------- quản trị
        if (path.equals("/admin") || path.startsWith("/admin/")) {
            if (user == null) {
                return veTrangDangNhap(request, response);
            }
            if (!user.isAdmin()) {
                return tuChoi(request, response, "/", "Bạn không có quyền truy cập khu quản trị.");
            }
            return true;
        }

        // ------------------------------------------------------- khu người bán
        if (path.equals("/seller") || path.startsWith("/seller/")) {
            if (user == null) {
                return veTrangDangNhap(request, response);
            }
            // Nộp hồ sơ và xem hồ sơ của mình: chỉ cần đăng nhập
            if (path.equals("/seller")
                    || path.equals("/seller/register")
                    || path.startsWith("/seller/registration")) {
                return true;
            }
            Long storeId = StoreContext.idTuDuongDan(request);
            if (storeId == null) {
                response.sendRedirect(request.getContextPath() + "/seller");
                return false;
            }
            Optional<StoreMember> tc = memberRepo.timDangLamViec(storeId, user.getId());
            if (tc.isEmpty()) {
                return tuChoi(request, response, "/seller", "Bạn không có quyền với gian hàng này.");
            }
            request.setAttribute(STORE_MEMBER, tc.get());
            return true;
        }

        // ------------------------------------------------------ cần đăng nhập
        if (canDangNhap(path) && user == null) {
            return veTrangDangNhap(request, response);
        }

        // --------------------------------------- quản trị không đi mua sắm
        if (user != null && user.isAdmin() && laDuongDanMuaSam(path)) {
            return tuChoi(request, response, "/admin",
                    "Tài khoản quản trị không sử dụng chức năng mua sắm.");
        }

        return true;
    }

    private boolean canDangNhap(String path) {
        return batDau(path, "/account")
                || batDau(path, "/orders")
                || batDau(path, "/checkout")
                || batDau(path, "/addresses")
                || batDau(path, "/notifications")
                || batDau(path, "/wishlist")
                || batDau(path, "/reviews")
                || batDau(path, "/support")
                || batDau(path, "/returns")
                || batDau(path, "/warranties");
    }

    private boolean laDuongDanMuaSam(String path) {
        return batDau(path, "/cart")
                || batDau(path, "/checkout")
                || batDau(path, "/orders")
                || batDau(path, "/wishlist")
                || batDau(path, "/addresses")
                || batDau(path, "/account");
    }

    private boolean batDau(String path, String tienTo) {
        return path.equals(tienTo) || path.startsWith(tienTo + "/");
    }

    private boolean veTrangDangNhap(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (Req.wantsJson(request)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Vui lòng đăng nhập để tiếp tục.\"}");
            return false;
        }
        if ("GET".equals(request.getMethod())) {
            String query = request.getQueryString();
            request.getSession(true).setAttribute(AuthService.INTENDED_URL,
                    request.getRequestURI() + (query != null ? "?" + query : ""));
        }
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    private boolean tuChoi(HttpServletRequest request, HttpServletResponse response,
                           String dich, String thongDiep) throws Exception {
        if (Req.wantsJson(request)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"" + thongDiep + "\"}");
            return false;
        }
        FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("error", thongDiep);
        RequestContextUtils.saveOutputFlashMap(request.getContextPath() + dich, request, response);
        response.sendRedirect(request.getContextPath() + dich);
        return false;
    }
}
