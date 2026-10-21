package com.smartwash.cart;

import com.smartwash.common.LoiNghiepVu;
import com.smartwash.product.Product;
import com.smartwash.product.ProductService;
import com.smartwash.store.Store;
import com.smartwash.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ giỏ hàng (đặc tả mục 5.7).
 *
 * Giỏ chỉ là nơi ghi nhớ ý định mua. Mọi kiểm tra ở đây đều mang tính "cảnh báo
 * sớm" chứ không phải chốt chặn: hàng có thể hết sau khi bỏ vào giỏ, nên lớp
 * đặt đơn vẫn phải kiểm tra lại tồn kho một lần nữa trong giao dịch (BR-09).
 */
@Service
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository itemRepo;
    private final ProductService productService;

    public CartService(CartRepository cartRepo,
                       CartItemRepository itemRepo,
                       ProductService productService) {
        this.cartRepo = cartRepo;
        this.itemRepo = itemRepo;
        this.productService = productService;
    }

    // ===================================================================
    // Đọc
    // ===================================================================

    /** Giỏ kèm dòng hàng để hiển thị; chưa có thì trả giỏ rỗng chưa lưu. */
    public Cart gioDeHienThi(Long userId) {
        return cartRepo.findDetailByUserId(userId).orElseGet(Cart::new);
    }

    public int soLuongTrenHeader(Long userId) {
        return cartRepo.demSoLuong(userId);
    }

    /**
     * Gom dòng hàng theo gian hàng.
     *
     * Việc gom này không chỉ để nhìn cho đẹp: một đơn đặt từ giỏ sẽ tách thành
     * nhiều đơn con, mỗi gian hàng một đơn (BR-11), vì mỗi gian tự đóng gói,
     * tự giao và tự chịu hoa hồng. Hiển thị theo nhóm ngay từ giỏ giúp khách
     * hiểu trước vì sao lát nữa họ nhận nhiều kiện hàng.
     */
    public List<NhomGianHang> nhomTheoGianHang(Cart gio) {
        Map<Long, NhomGianHang> theoId = new LinkedHashMap<>();
        if (gio == null || gio.getItems() == null) {
            return List.of();
        }
        for (CartItem dong : gio.getItems()) {
            Store s = dong.getStore();
            Long khoa = s == null ? -1L : s.getId();
            theoId.computeIfAbsent(khoa, k -> new NhomGianHang(s)).items.add(dong);
        }
        return new ArrayList<>(theoId.values());
    }

    // ===================================================================
    // Ghi
    // ===================================================================

    @Transactional
    public void them(User nguoiDung, Long productId, int soLuong) {
        if (soLuong < 1) {
            soLuong = 1;
        }
        Product sp = productService.timHoacBao(productId);
        kiemTraMuaDuoc(sp);

        Cart gio = layHoacTao(nguoiDung);

        CartItem dong = itemRepo.findByCartIdAndProductId(gio.getId(), productId).orElse(null);
        int moi = (dong == null ? 0 : dong.getQuantity()) + soLuong;
        kiemTraSoLuong(sp, moi);

        if (dong == null) {
            dong = new CartItem();
            dong.setCart(gio);
            dong.setProduct(sp);
            dong.setQuantity(moi);
        } else {
            dong.setQuantity(moi);
        }
        itemRepo.save(dong);
    }

    @Transactional
    public void doiSoLuong(User nguoiDung, Long itemId, int soLuong) {
        CartItem dong = dongCuaToi(nguoiDung, itemId);
        if (soLuong < 1) {
            itemRepo.delete(dong);
            return;
        }
        kiemTraSoLuong(dong.getProduct(), soLuong);
        dong.setQuantity(soLuong);
        itemRepo.save(dong);
    }

    @Transactional
    public void xoa(User nguoiDung, Long itemId) {
        itemRepo.delete(dongCuaToi(nguoiDung, itemId));
    }

    @Transactional
    public void xoaHet(User nguoiDung) {
        cartRepo.findByUserId(nguoiDung.getId()).ifPresent(gio -> {
            gio.getItems().clear();
            cartRepo.save(gio);
        });
    }

    /** Bỏ hết các dòng đang vướng, để khách đặt phần còn lại mà không phải xoá tay. */
    @Transactional
    public int xoaDongVuong(User nguoiDung) {
        Cart gio = cartRepo.findDetailByUserId(nguoiDung.getId()).orElse(null);
        if (gio == null) {
            return 0;
        }
        List<CartItem> bo = gio.getItems().stream().filter(d -> !d.isPurchasable()).toList();
        for (CartItem d : bo) {
            itemRepo.delete(d);
        }
        return bo.size();
    }

    // ===================================================================
    // Nội bộ
    // ===================================================================

    @Transactional
    public Cart layHoacTao(User nguoiDung) {
        return cartRepo.findByUserId(nguoiDung.getId()).orElseGet(() -> {
            Cart gio = new Cart();
            gio.setUser(nguoiDung);
            return cartRepo.save(gio);
        });
    }

    private CartItem dongCuaToi(User nguoiDung, Long itemId) {
        CartItem dong = itemRepo.findById(itemId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy dòng hàng này trong giỏ."));
        // Không dùng itemId từ URL mà không kiểm chủ sở hữu: nếu không, đổi số
        // trên thanh địa chỉ là sửa được giỏ của người khác.
        if (dong.getCart() == null
                || dong.getCart().getUser() == null
                || !dong.getCart().getUser().getId().equals(nguoiDung.getId())) {
            throw new LoiNghiepVu("Dòng hàng này không thuộc giỏ của bạn.");
        }
        return dong;
    }

    private void kiemTraMuaDuoc(Product sp) {
        if (!sp.isSellable()) {
            throw new LoiNghiepVu("Sản phẩm này hiện không bán.");
        }
        if (sp.getSellableQuantity() <= 0) {
            throw new LoiNghiepVu("Sản phẩm này đã hết hàng.");
        }
    }

    private void kiemTraSoLuong(Product sp, int soLuong) {
        if (soLuong > CartItem.SO_LUONG_TOI_DA) {
            throw new LoiNghiepVu("Mỗi sản phẩm chỉ đặt tối đa "
                    + CartItem.SO_LUONG_TOI_DA + " cái trong một đơn.");
        }
        int con = sp.getSellableQuantity();
        if (soLuong > con) {
            throw new LoiNghiepVu(con <= 0
                    ? "Sản phẩm này đã hết hàng."
                    : "Chỉ còn " + con + " sản phẩm.");
        }
    }

    // ===================================================================
    // Kiểu phụ
    // ===================================================================

    /** Một nhóm dòng hàng cùng gian hàng, dùng cho trang giỏ và trang đặt hàng. */
    public static class NhomGianHang {

        private final Store store;
        private final List<CartItem> items = new ArrayList<>();

        public NhomGianHang(Store store) {
            this.store = store;
        }

        public Store getStore() { return store; }
        public List<CartItem> getItems() { return items; }

        public String getStoreName() {
            return store == null ? "Gian hàng không xác định" : store.getName();
        }

        public BigDecimal getSubtotal() {
            BigDecimal tong = BigDecimal.ZERO;
            for (CartItem i : items) {
                if (i.isPurchasable()) {
                    tong = tong.add(i.getSubtotal());
                }
            }
            return tong;
        }

        public int getTotalQuantity() {
            int tong = 0;
            for (CartItem i : items) {
                tong += i.getQuantity() == null ? 0 : i.getQuantity();
            }
            return tong;
        }
    }
}
