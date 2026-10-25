package com.smartwash.order;

import com.smartwash.cart.Cart;
import com.smartwash.cart.CartItem;
import com.smartwash.cart.CartService;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.inventory.Inventory;
import com.smartwash.inventory.InventoryRepository;
import com.smartwash.product.Product;
import com.smartwash.setting.SettingService;
import com.smartwash.store.Store;
import com.smartwash.user.Address;
import com.smartwash.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ đặt hàng (đặc tả mục 5.8, BR-09 đến BR-12).
 *
 * Toàn bộ việc đặt hàng nằm trong MỘT giao dịch: kiểm tồn, giữ chỗ, tạo đơn,
 * tạo bản ghi thanh toán, xoá giỏ. Hỏng bất cứ bước nào thì cuộn lại hết —
 * không được phép có đơn đã tạo mà tồn kho chưa trừ, hay giỏ đã xoá mà đơn
 * chưa lưu.
 */
@Service
public class OrderService {

    /**
     * Phí giao hàng tính theo TỪNG gian hàng, vì mỗi gian tự gửi một kiện
     * riêng — mua ba gian là ba lần vận chuyển, không phải một.
     *
     * Hai con số dưới đây là tạm tính cho bản demo. Khi nối với đơn vị vận
     * chuyển thật (GHN, GHTK...) thì phí phải lấy theo cân nặng, kích thước và
     * khoảng cách qua API của họ; lúc đó bỏ hai hằng số này đi.
     */
    private static final BigDecimal PHI_GIAO_MOI_GIAN_HANG = new BigDecimal("300000");
    private static final BigDecimal NGUONG_MIEN_PHI_GIAO   = new BigDecimal("5000000");

    private static final String BANG_CHU = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom NGAU_NHIEN = new SecureRandom();

    private final OrderRepository orderRepo;
    private final StoreOrderRepository storeOrderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final PaymentRepository paymentRepo;
    private final InventoryRepository inventoryRepo;
    private final CartService cartService;
    private final SettingService settingService;

    public OrderService(OrderRepository orderRepo,
                        StoreOrderRepository storeOrderRepo,
                        OrderStatusHistoryRepository historyRepo,
                        PaymentRepository paymentRepo,
                        InventoryRepository inventoryRepo,
                        CartService cartService,
                        SettingService settingService) {
        this.orderRepo = orderRepo;
        this.storeOrderRepo = storeOrderRepo;
        this.historyRepo = historyRepo;
        this.paymentRepo = paymentRepo;
        this.inventoryRepo = inventoryRepo;
        this.cartService = cartService;
        this.settingService = settingService;
    }

    // ===================================================================
    // Đặt hàng
    // ===================================================================

    /**
     * Chốt giỏ hàng thành đơn.
     *
     * Thứ tự các bước có chủ đích:
     *  1. Gom giỏ theo gian hàng — quyết định sẽ sinh ra mấy đơn con.
     *  2. Với từng dòng: KHOÁ dòng tồn kho rồi mới kiểm tra và giữ chỗ.
     *     Kiểm tra ở trang giỏ chỉ là cảnh báo sớm; chốt chặn thật là ở đây,
     *     bên trong giao dịch và sau khi đã khoá dòng.
     *  3. Chụp dữ liệu sản phẩm vào order_items (BR-10).
     *  4. Tính tiền và hoa hồng cho từng đơn con, rồi cộng lên đơn tổng.
     *  5. Tạo bản ghi thanh toán, ghi lịch sử trạng thái, xoá giỏ.
     */
    @Transactional
    public Order datHang(User khach, Address diaChi, String hinhThucThanhToan, String ghiChu) {
        Cart gio = cartService.gioDeHienThi(khach.getId());
        if (gio == null || gio.getItems() == null || gio.getItems().isEmpty()) {
            throw new LoiNghiepVu("Giỏ hàng đang trống.");
        }
        if (diaChi == null) {
            throw new LoiNghiepVu("Hãy chọn địa chỉ nhận hàng.");
        }
        String httt = chuanHoaHinhThuc(hinhThucThanhToan);

        Order don = new Order();
        don.setOrderCode(sinhMaDon());
        don.setCustomer(khach);
        chepDiaChi(don, diaChi);
        don.setNote(trong(ghiChu) ? null : ghiChu.trim());
        don.setPaymentMethod(httt);
        don.setPaymentStatus(Order.UNPAID);
        don.setStatus(Order.PENDING_CONFIRMATION);

        BigDecimal tyLeHoaHong = settingService.tyLeHoaHong();
        BigDecimal tongHang = BigDecimal.ZERO;
        BigDecimal tongPhiGiao = BigDecimal.ZERO;

        int thuTu = 0;
        for (Map.Entry<Long, List<CartItem>> nhom : gomTheoGianHang(gio).entrySet()) {
            List<CartItem> dongCuaGian = nhom.getValue();
            Store gianHang = dongCuaGian.get(0).getStore();
            if (gianHang == null) {
                throw new LoiNghiepVu("Có sản phẩm trong giỏ không còn thuộc gian hàng nào.");
            }

            StoreOrder donCon = new StoreOrder();
            donCon.setStore(gianHang);
            donCon.setStoreOrderCode(don.getOrderCode() + "-" + (++thuTu));
            donCon.setStatus(StoreOrder.PENDING_CONFIRMATION);

            BigDecimal hangCuaGian = BigDecimal.ZERO;
            for (CartItem dong : dongCuaGian) {
                Product sp = dong.getProduct();
                int soLuong = dong.getQuantity() == null ? 0 : dong.getQuantity();
                if (soLuong < 1) {
                    continue;
                }
                giuCho(sp, soLuong);

                OrderItem mon = chupLaiSanPham(sp, soLuong);
                donCon.addItem(mon);
                hangCuaGian = hangCuaGian.add(mon.getSubtotal());
            }
            if (donCon.getItems().isEmpty()) {
                continue;
            }

            BigDecimal phiGiao = phiGiaoHang(hangCuaGian);
            tinhTienDonCon(donCon, hangCuaGian, phiGiao, tyLeHoaHong);

            don.addStoreOrder(donCon);
            tongHang = tongHang.add(hangCuaGian);
            tongPhiGiao = tongPhiGiao.add(phiGiao);
        }

        if (don.getStoreOrders().isEmpty()) {
            throw new LoiNghiepVu("Không có sản phẩm nào trong giỏ đặt được.");
        }

        don.setItemsSubtotal(tongHang);
        don.setShippingFeeTotal(tongPhiGiao);
        don.setGrandTotal(tongHang.add(tongPhiGiao));
        orderRepo.save(don);

        for (StoreOrder donCon : don.getStoreOrders()) {
            historyRepo.save(new OrderStatusHistory(
                    donCon, null, StoreOrder.PENDING_CONFIRMATION, khach, "Khách đặt hàng"));
        }

        taoBanGhiThanhToan(don);
        cartService.xoaHet(khach);
        return don;
    }

    /**
     * Giữ chỗ tồn kho cho một dòng hàng (BR-09).
     *
     * Cộng vào quantity_reserved chứ KHÔNG trừ quantity_available. Lý do:
     * hàng vẫn đang nằm trong kho của người bán, chưa đi đâu cả — chỉ là đã
     * có người đặt nên không bán cho người khác nữa. Số bán được của một sản
     * phẩm là available - reserved.
     *
     * Trừ hẳn available chỉ xảy ra khi giao hàng thành công; nếu đơn bị huỷ
     * thì chỉ cần nhả reserved ra, kho không phải nhập lại.
     */
    private void giuCho(Product sp, int soLuong) {
        if (sp == null) {
            throw new LoiNghiepVu("Một sản phẩm trong giỏ không còn tồn tại.");
        }
        if (!sp.isSellable()) {
            throw new LoiNghiepVu("Sản phẩm \"" + sp.getName() + "\" hiện không bán.");
        }
        Inventory ton = inventoryRepo.khoaDeDatHang(sp.getId())
                .orElseThrow(() -> new LoiNghiepVu(
                        "Sản phẩm \"" + sp.getName() + "\" chưa khai báo tồn kho."));

        int conBan = ton.getSellable();
        if (conBan < soLuong) {
            throw new LoiNghiepVu(conBan <= 0
                    ? "Sản phẩm \"" + sp.getName() + "\" đã hết hàng."
                    : "Sản phẩm \"" + sp.getName() + "\" chỉ còn " + conBan + " cái.");
        }
        ton.setQuantityReserved((ton.getQuantityReserved() == null ? 0 : ton.getQuantityReserved()) + soLuong);
        inventoryRepo.save(ton);
    }

    /** Nhả lại phần đã giữ khi đơn bị huỷ. */
    private void nhaCho(Long productId, int soLuong) {
        inventoryRepo.khoaDeDatHang(productId).ifPresent(ton -> {
            int dangGiu = ton.getQuantityReserved() == null ? 0 : ton.getQuantityReserved();
            ton.setQuantityReserved(Math.max(0, dangGiu - soLuong));
            inventoryRepo.save(ton);
        });
    }

    /** Chụp lại thông tin sản phẩm vào dòng đơn (BR-10). */
    private OrderItem chupLaiSanPham(Product sp, int soLuong) {
        OrderItem mon = new OrderItem();
        mon.setProduct(sp);
        mon.setProductName(sp.getName());
        mon.setModelCode(sp.getModelCode());
        mon.setBrandName(sp.getBrandName());
        mon.setImage(sp.getImageUrl());
        mon.setWarrantyMonths(sp.getWarrantyMonths() == null ? 0 : sp.getWarrantyMonths());
        mon.setQuantity(soLuong);

        BigDecimal donGia = sp.getEffectivePrice();
        mon.setUnitPrice(donGia);
        mon.setSubtotal(donGia.multiply(BigDecimal.valueOf(soLuong)));
        return mon;
    }

    /**
     * Tính tiền và hoa hồng cho một đơn con.
     *
     * Hoa hồng tính trên TIỀN HÀNG, không tính trên phí vận chuyển: tiền ship
     * là khoản trả cho đơn vị giao hàng chứ không phải doanh thu của người
     * bán, sàn ăn phần trăm trên đó là vô lý.
     *
     * Tỉ lệ được chép vào commission_rate của đơn, không đọc lại từ cấu hình
     * khi xem sau này — quản trị viên đổi tỉ lệ tháng sau thì đơn cũ vẫn phải
     * đối soát theo tỉ lệ lúc giao dịch.
     */
    private void tinhTienDonCon(StoreOrder donCon, BigDecimal tienHang,
                                BigDecimal phiGiao, BigDecimal tyLe) {
        donCon.setItemsSubtotal(tienHang);
        donCon.setShippingFee(phiGiao);
        donCon.setTotalAmount(tienHang.add(phiGiao));

        BigDecimal hoaHong = tienHang
                .multiply(tyLe)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        donCon.setCommissionRate(tyLe);
        donCon.setCommissionAmount(hoaHong);
        donCon.setSellerAmount(donCon.getTotalAmount().subtract(hoaHong));
    }

    private BigDecimal phiGiaoHang(BigDecimal tienHangCuaGian) {
        return tienHangCuaGian.compareTo(NGUONG_MIEN_PHI_GIAO) >= 0
                ? BigDecimal.ZERO
                : PHI_GIAO_MOI_GIAN_HANG;
    }

    /**
     * Bản ghi thanh toán ban đầu.
     *
     * COD thì để UNPAID, tiền thu khi giao. Cổng trực tuyến thì để PENDING và
     * chờ cổng báo về; phần nối cổng thật thuộc giai đoạn sau, hiện chỉ dựng
     * sẵn bản ghi để luồng đối soát không phải sửa lại cấu trúc.
     */
    private void taoBanGhiThanhToan(Order don) {
        Payment tt = new Payment();
        tt.setOrder(don);
        tt.setProvider(don.getPaymentMethod());
        tt.setAmount(don.getGrandTotal());
        tt.setStatus(Order.COD.equals(don.getPaymentMethod()) ? Order.UNPAID : Order.PENDING);
        tt.setIdempotencyKey(don.getOrderCode() + ":tao");
        paymentRepo.save(tt);

        don.setPaymentStatus(tt.getStatus());
    }

    // ===================================================================
    // Huỷ đơn
    // ===================================================================

    /**
     * Khách tự huỷ cả đơn.
     *
     * Chỉ cho phép khi mọi đơn con còn ở trạng thái chờ xác nhận. Huỷ xong
     * phải nhả lại toàn bộ tồn kho đã giữ, nếu không thì hàng bị khoá vĩnh
     * viễn và không ai mua được nữa.
     */
    @Transactional
    public void khachHuyDon(User khach, String maDon, String lyDo) {
        Order don = orderRepo.findDetailByOrderCode(maDon)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy đơn hàng."));
        kiemTraLaDonCuaToi(don, khach);

        if (!don.isCancellableByCustomer()) {
            throw new LoiNghiepVu(
                    "Đơn đã được gian hàng xác nhận nên không tự huỷ được. "
                    + "Vui lòng liên hệ gian hàng để yêu cầu huỷ.");
        }

        LocalDateTime bayGio = LocalDateTime.now();
        for (StoreOrder donCon : don.getStoreOrders()) {
            for (OrderItem mon : donCon.getItems()) {
                if (mon.getProduct() != null) {
                    nhaCho(mon.getProduct().getId(), mon.getQuantity() == null ? 0 : mon.getQuantity());
                }
            }
            String truoc = donCon.getStatus();
            donCon.setStatus(StoreOrder.CANCELLED);
            donCon.setCancelledAt(bayGio);
            donCon.setCancelReason(trong(lyDo) ? "Khách tự huỷ" : lyDo.trim());
            storeOrderRepo.save(donCon);

            historyRepo.save(new OrderStatusHistory(
                    donCon, truoc, StoreOrder.CANCELLED, khach, donCon.getCancelReason()));
        }

        don.setStatus(Order.CANCELLED);
        don.setCancelledAt(bayGio);
        orderRepo.save(don);
    }

    // ===================================================================
    // Tra cứu
    // ===================================================================

    public Page<Order> cuaKhach(Long userId, int trang, int moiTrang) {
        return orderRepo.findByCustomerIdOrderByPlacedAtDesc(
                userId, PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    public Order chiTiet(User khach, String maDon) {
        Order don = orderRepo.findDetailByOrderCode(maDon)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy đơn hàng."));
        kiemTraLaDonCuaToi(don, khach);
        return don;
    }

    public List<OrderStatusHistory> lichSu(Long storeOrderId) {
        return historyRepo.findByStoreOrderIdOrderByCreatedAtAscIdAsc(storeOrderId);
    }

    /**
     * Trạng thái đơn tổng suy ra từ các đơn con.
     *
     * Quy tắc: mọi đơn con cùng một trạng thái thì đơn tổng lấy trạng thái đó;
     * khác nhau thì lấy trạng thái "chậm nhất" — khách chỉ coi là nhận đủ hàng
     * khi kiện cuối cùng đã tới. Huỷ hết mới tính là đơn huỷ; huỷ một phần thì
     * đơn tổng vẫn đang chạy theo phần còn lại.
     */
    public static String trangThaiDonTong(List<StoreOrder> donCon) {
        if (donCon == null || donCon.isEmpty()) {
            return Order.PENDING_CONFIRMATION;
        }
        boolean huyHet = true;
        int thapNhat = Integer.MAX_VALUE;
        for (StoreOrder so : donCon) {
            if (!StoreOrder.CANCELLED.equals(so.getStatus())) {
                huyHet = false;
                thapNhat = Math.min(thapNhat, bac(so.getStatus()));
            }
        }
        if (huyHet) {
            return Order.CANCELLED;
        }
        return switch (thapNhat) {
            case 1  -> Order.PROCESSING;
            case 2  -> Order.SHIPPING;
            case 3  -> Order.DELIVERED;
            case 4  -> Order.COMPLETED;
            case 5  -> Order.RETURNED;
            default -> Order.PENDING_CONFIRMATION;
        };
    }

    private static int bac(String trangThaiDonCon) {
        return switch (trangThaiDonCon == null ? "" : trangThaiDonCon) {
            case StoreOrder.CONFIRMED, StoreOrder.PROCESSING -> 1;
            case StoreOrder.SHIPPED   -> 2;
            case StoreOrder.DELIVERED -> 3;
            case StoreOrder.COMPLETED -> 4;
            case StoreOrder.RETURNED  -> 5;
            default -> 0;
        };
    }

    // ===================================================================
    // Nội bộ
    // ===================================================================

    private void kiemTraLaDonCuaToi(Order don, User khach) {
        // Mã đơn nằm trên URL nên phải đối chiếu chủ đơn, nếu không thì đoán
        // mã là xem được đơn của người khác kèm tên, số điện thoại, địa chỉ.
        if (don.getCustomer() == null || !don.getCustomer().getId().equals(khach.getId())) {
            throw new LoiNghiepVu("Không tìm thấy đơn hàng.");
        }
    }

    private Map<Long, List<CartItem>> gomTheoGianHang(Cart gio) {
        Map<Long, List<CartItem>> theoGian = new LinkedHashMap<>();
        for (CartItem dong : gio.getItems()) {
            Store s = dong.getStore();
            Long khoa = s == null ? -1L : s.getId();
            theoGian.computeIfAbsent(khoa, k -> new ArrayList<>()).add(dong);
        }
        return theoGian;
    }

    private void chepDiaChi(Order don, Address dc) {
        don.setReceiverName(dc.getReceiverName());
        don.setReceiverPhone(dc.getPhone());
        don.setAddressLine(dc.getLine());
        don.setProvinceId(dc.getProvinceId());
        don.setProvinceName(dc.getProvinceName());
        don.setDistrictId(dc.getDistrictId());
        don.setDistrictName(dc.getDistrictName());
        don.setWardCode(dc.getWardCode());
        don.setWardName(dc.getWardName());
    }

    private String chuanHoaHinhThuc(String httt) {
        if (httt == null) {
            return Order.COD;
        }
        return switch (httt.trim().toUpperCase()) {
            case Order.VNPAY     -> Order.VNPAY;
            case Order.MOMO      -> Order.MOMO;
            case Order.BANK_DEMO -> Order.BANK_DEMO;
            default              -> Order.COD;
        };
    }

    /**
     * Mã đơn: SW + ngày + 5 ký tự ngẫu nhiên, ví dụ SW261021KQ7MF.
     *
     * Không dùng số tăng dần vì mã nằm trên URL: mã chạy liên tiếp thì người
     * ngoài đoán được mã đơn kế tiếp, và còn lộ ra mỗi ngày sàn bán bao nhiêu
     * đơn. Bảng chữ cái đã bỏ các ký tự dễ nhìn nhầm (0/O, 1/I).
     */
    private String sinhMaDon() {
        String ngay = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        for (int lan = 0; lan < 20; lan++) {
            StringBuilder sb = new StringBuilder("SW").append(ngay);
            for (int i = 0; i < 5; i++) {
                sb.append(BANG_CHU.charAt(NGAU_NHIEN.nextInt(BANG_CHU.length())));
            }
            String ma = sb.toString();
            if (!orderRepo.existsByOrderCode(ma)) {
                return ma;
            }
        }
        throw new LoiNghiepVu("Không sinh được mã đơn, vui lòng thử lại.");
    }

    private boolean trong(String s) {
        return s == null || s.isBlank();
    }
}
