package com.smartwash.order;

import com.smartwash.common.LoiNghiepVu;
import com.smartwash.inventory.InventoryRepository;
import com.smartwash.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Người bán xử lý đơn của gian hàng mình (đặc tả mục 6.7).
 *
 * Tách khỏi {@link OrderService} vì hai bên nhìn đơn theo hai góc khác nhau:
 * khách thao tác trên ĐƠN TỔNG (huỷ cả đơn), người bán thao tác trên ĐƠN CON
 * của riêng họ (BR-11). Gộp chung một lớp thì mỗi phương thức lại phải hỏi
 * "ai đang gọi" để quyết định phạm vi — đó là dấu hiệu nên tách.
 */
@Service
public class SellerOrderService {

    /**
     * Máy trạng thái: từ mỗi trạng thái chỉ đi được sang những trạng thái liệt kê.
     *
     * Khai báo thành bảng thay vì rải if-else khắp controller, vì ba lý do:
     * đọc một chỗ là biết toàn bộ luồng; thêm trạng thái mới chỉ sửa một dòng;
     * và không sót trường hợp — ví dụ không ai "giao hàng" được một đơn đã huỷ
     * dù có tự gõ URL.
     */
    private static final Map<String, Set<String>> CHUYEN_DUOC = Map.of(
            StoreOrder.PENDING_CONFIRMATION, Set.of(StoreOrder.CONFIRMED, StoreOrder.CANCELLED),
            StoreOrder.CONFIRMED,            Set.of(StoreOrder.PROCESSING, StoreOrder.CANCELLED),
            StoreOrder.PROCESSING,           Set.of(StoreOrder.SHIPPED, StoreOrder.CANCELLED),
            StoreOrder.SHIPPED,              Set.of(StoreOrder.DELIVERED),
            StoreOrder.DELIVERED,            Set.of(StoreOrder.COMPLETED),
            StoreOrder.COMPLETED,            Set.of(),
            StoreOrder.CANCELLED,            Set.of(),
            StoreOrder.RETURNED,             Set.of());

    private final StoreOrderRepository storeOrderRepo;
    private final OrderRepository orderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final PaymentRepository paymentRepo;
    private final InventoryRepository inventoryRepo;

    public SellerOrderService(StoreOrderRepository storeOrderRepo,
                              OrderRepository orderRepo,
                              OrderStatusHistoryRepository historyRepo,
                              PaymentRepository paymentRepo,
                              InventoryRepository inventoryRepo) {
        this.storeOrderRepo = storeOrderRepo;
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.paymentRepo = paymentRepo;
        this.inventoryRepo = inventoryRepo;
    }

    // ===================================================================
    // Đọc
    // ===================================================================

    public Page<StoreOrder> danhSach(Long storeId, String trangThai, String tuKhoa,
                                     int trang, int moiTrang) {
        return storeOrderRepo.cuaGianHang(storeId, rong(trangThai), rong(tuKhoa),
                PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    public long soChoXuLy(Long storeId) {
        return storeOrderRepo.demChoXuLy(storeId);
    }

    /**
     * Lấy một đơn con, đối chiếu đúng gian hàng.
     *
     * Mã đơn nằm trên URL nên bắt buộc kiểm tra: không có bước này thì chủ gian
     * hàng A gõ mã đơn của gian hàng B là xem được tên, số điện thoại và địa
     * chỉ khách của đối thủ.
     */
    public StoreOrder chiTiet(Long storeId, String maDonCon) {
        StoreOrder so = storeOrderRepo.findDetailByStoreOrderCode(maDonCon)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy đơn hàng."));
        if (so.getStore() == null || !so.getStore().getId().equals(storeId)) {
            throw new LoiNghiepVu("Không tìm thấy đơn hàng.");
        }
        return so;
    }

    public List<OrderStatusHistory> lichSu(Long storeOrderId) {
        return historyRepo.findByStoreOrderIdOrderByCreatedAtAscIdAsc(storeOrderId);
    }

    public Set<String> buocTiepTheo(StoreOrder so) {
        return CHUYEN_DUOC.getOrDefault(so.getStatus(), Set.of());
    }

    // ===================================================================
    // Đổi trạng thái
    // ===================================================================

    /**
     * Chuyển một đơn con sang trạng thái mới.
     *
     * Mọi thay đổi đều đi qua đây — không chỗ nào được phép gán thẳng
     * {@code so.setStatus(...)}. Tập trung một cửa thì ba việc luôn xảy ra
     * cùng nhau và không bao giờ quên: kiểm tra bước chuyển hợp lệ, ghi nhật
     * ký, và cập nhật lại trạng thái đơn tổng.
     */
    @Transactional
    public StoreOrder doiTrangThai(Long storeId, String maDonCon, String trangThaiMoi,
                                   User nguoiLam, String ghiChu) {
        StoreOrder so = chiTiet(storeId, maDonCon);
        String truoc = so.getStatus();

        if (!CHUYEN_DUOC.getOrDefault(truoc, Set.of()).contains(trangThaiMoi)) {
            throw new LoiNghiepVu("Không thể chuyển đơn từ \"" + so.getStatusLabel()
                    + "\" sang trạng thái này.");
        }

        LocalDateTime bayGio = LocalDateTime.now();
        so.setStatus(trangThaiMoi);

        switch (trangThaiMoi) {
            case StoreOrder.CONFIRMED  -> so.setConfirmedAt(bayGio);
            case StoreOrder.SHIPPED    -> so.setShippedAt(bayGio);
            case StoreOrder.DELIVERED  -> {
                so.setDeliveredAt(bayGio);
                chotTonKho(so);
            }
            case StoreOrder.COMPLETED  -> so.setCompletedAt(bayGio);
            case StoreOrder.CANCELLED  -> {
                so.setCancelledAt(bayGio);
                so.setCancelReason(rong(ghiChu) == null ? "Gian hàng huỷ" : ghiChu.trim());
                nhaTonKho(so);
            }
            default -> { }
        }
        storeOrderRepo.save(so);

        historyRepo.save(new OrderStatusHistory(so, truoc, trangThaiMoi, nguoiLam, rong(ghiChu)));
        capNhatDonTong(so.getOrder());
        return so;
    }

    /**
     * Giao hàng thành công thì chốt tồn kho.
     *
     * Lúc đặt hàng chỉ cộng vào quantity_reserved — hàng vẫn nằm trong kho,
     * chỉ là không bán cho người khác nữa (BR-09). Đến đây hàng mới thật sự
     * rời kho, nên trừ cả available lẫn reserved. Hai số cùng giảm nên
     * sellable = available - reserved không đổi, đúng với thực tế: số còn bán
     * được không thay đổi khi một đơn đã giữ chỗ nay đi giao.
     *
     * Chốt đúng một lần, tại bước SHIPPED -> DELIVERED. Máy trạng thái bảo
     * đảm không vào đây hai lần cho cùng một đơn.
     */
    private void chotTonKho(StoreOrder so) {
        for (OrderItem mon : so.getItems()) {
            if (mon.getProduct() == null) {
                continue;
            }
            int sl = mon.getQuantity() == null ? 0 : mon.getQuantity();
            inventoryRepo.khoaDeDatHang(mon.getProduct().getId()).ifPresent(ton -> {
                ton.setQuantityAvailable(Math.max(0, soNguyen(ton.getQuantityAvailable()) - sl));
                ton.setQuantityReserved(Math.max(0, soNguyen(ton.getQuantityReserved()) - sl));
                inventoryRepo.save(ton);
            });
        }
    }

    /** Huỷ đơn thì nhả phần đã giữ, hàng quay lại bán được ngay. */
    private void nhaTonKho(StoreOrder so) {
        for (OrderItem mon : so.getItems()) {
            if (mon.getProduct() == null) {
                continue;
            }
            int sl = mon.getQuantity() == null ? 0 : mon.getQuantity();
            inventoryRepo.khoaDeDatHang(mon.getProduct().getId()).ifPresent(ton -> {
                ton.setQuantityReserved(Math.max(0, soNguyen(ton.getQuantityReserved()) - sl));
                inventoryRepo.save(ton);
            });
        }
    }

    /**
     * Tính lại trạng thái đơn tổng từ các đơn con, và thu tiền COD khi xong.
     *
     * Đơn tổng không có trạng thái của riêng nó — nó là giá trị suy ra. Gọi
     * lại sau mỗi lần một đơn con đổi, nếu không thì khách mở đơn ra vẫn thấy
     * "Chờ xác nhận" trong khi hàng đã giao tới nhà.
     */
    private void capNhatDonTong(Order don) {
        if (don == null) {
            return;
        }
        List<StoreOrder> cacDonCon = storeOrderRepo.findByOrderId(don.getId());
        String moi = OrderService.trangThaiDonTong(cacDonCon);
        don.setStatus(moi);

        if (Order.COMPLETED.equals(moi)) {
            don.setCompletedAt(LocalDateTime.now());
            thuTienCod(don);
        } else if (Order.CANCELLED.equals(moi) && don.getCancelledAt() == null) {
            don.setCancelledAt(LocalDateTime.now());
        }
        orderRepo.save(don);
    }

    /**
     * Đơn COD hoàn tất nghĩa là nhân viên giao hàng đã thu được tiền.
     *
     * Đơn trả trước qua cổng thì KHÔNG đụng vào đây: trạng thái thanh toán của
     * chúng do cổng báo về quyết định, không phải do việc giao hàng (BR-12).
     */
    private void thuTienCod(Order don) {
        if (!Order.COD.equals(don.getPaymentMethod()) || Order.PAID.equals(don.getPaymentStatus())) {
            return;
        }
        LocalDateTime bayGio = LocalDateTime.now();
        don.setPaymentStatus(Order.PAID);
        for (Payment tt : paymentRepo.findByOrderIdOrderByIdDesc(don.getId())) {
            if (!Order.PAID.equals(tt.getStatus())) {
                tt.setStatus(Order.PAID);
                tt.setPaidAt(bayGio);
                paymentRepo.save(tt);
            }
        }
    }

    // ===================================================================
    // Nội bộ
    // ===================================================================

    private int soNguyen(Integer n) {
        return n == null ? 0 : n;
    }

    private String rong(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
