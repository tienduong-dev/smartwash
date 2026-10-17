package com.smartwash.store;

import com.smartwash.common.LoiNghiepVu;
import com.smartwash.user.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Xác định người bán đang làm việc với gian hàng nào.
 *
 * Mọi đường dẫn khu người bán đều mang id gian hàng: /seller/{storeId}/...
 * Cách này rõ ràng hơn lưu "gian hàng hiện tại" trong phiên, vì một người có
 * thể mở hai tab cho hai gian hàng mà không giẫm lên nhau.
 */
@Service
public class StoreContext {

    private final StoreMemberRepository memberRepo;
    private final StoreRepository storeRepo;

    public StoreContext(StoreMemberRepository memberRepo, StoreRepository storeRepo) {
        this.memberRepo = memberRepo;
        this.storeRepo = storeRepo;
    }

    /** Tư cách thành viên của người này ở gian hàng này; ném lỗi nếu không có. */
    public StoreMember batBuocThanhVien(Long storeId, User user) {
        if (user == null) {
            throw new LoiNghiepVu("Vui lòng đăng nhập.");
        }
        return memberRepo.timDangLamViec(storeId, user.getId())
                .orElseThrow(() -> new LoiNghiepVu("Bạn không có quyền với gian hàng này."));
    }

    public Store batBuocGianHang(Long storeId, User user) {
        batBuocThanhVien(storeId, user);
        return storeRepo.findDetailById(storeId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy gian hàng."));
    }

    /** Rút storeId từ đường dẫn /seller/{id}/... ; null nếu không có. */
    public static Long idTuDuongDan(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/seller/")) {
            return null;
        }
        String con = path.substring("/seller/".length());
        int gach = con.indexOf('/');
        String doan = gach < 0 ? con : con.substring(0, gach);
        try {
            return Long.parseLong(doan);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Store> gianHangCua(User user) {
        return user == null ? List.of() : storeRepo.cuaNguoiDung(user.getId());
    }
}
