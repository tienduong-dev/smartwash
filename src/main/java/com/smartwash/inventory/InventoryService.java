package com.smartwash.inventory;

import com.smartwash.common.LoiNghiepVu;
import com.smartwash.product.Product;
import com.smartwash.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tồn kho (BR-09).
 *
 * Giai đoạn này chỉ có phần đặt số lượng và cảnh báo sắp hết. Việc giữ chỗ
 * khi đặt hàng và nhả chỗ khi huỷ thuộc giai đoạn đơn hàng, sẽ thêm hàm
 * giuCho/nhaCho vào đây.
 */
@Service
public class InventoryService {

    private final InventoryRepository repo;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository repo, ProductRepository productRepository) {
        this.repo = repo;
        this.productRepository = productRepository;
    }

    /** Đặt lại tồn kho. Không cho đặt thấp hơn phần đang giữ chỗ. */
    @Transactional
    public Inventory datTon(Long productId, Long storeId, int soLuong, Integer nguongCanhBao, Long giaVon) {
        Product p = productRepository.findDetailById(productId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy sản phẩm."));
        if (p.getStore() == null || !p.getStore().getId().equals(storeId)) {
            throw new LoiNghiepVu("Sản phẩm này không thuộc gian hàng của bạn.");
        }
        if (soLuong < 0) {
            throw new LoiNghiepVu("quantity", "Số lượng tồn không được âm.");
        }

        Inventory kho = p.getInventory();
        if (kho == null) {
            kho = new Inventory();
            kho.setProduct(p);
            p.setInventory(kho);
        }

        int dangGiu = kho.getQuantityReserved() == null ? 0 : kho.getQuantityReserved();
        if (soLuong < dangGiu) {
            throw new LoiNghiepVu("quantity", "Đang có " + dangGiu
                    + " sản phẩm giữ chỗ cho đơn chưa hoàn tất, không đặt tồn thấp hơn số này được.");
        }

        kho.setQuantityAvailable(soLuong);
        if (nguongCanhBao != null) {
            kho.setMinStock(Math.max(0, nguongCanhBao));
        }
        if (giaVon != null) {
            kho.setCostPrice(Math.max(0L, giaVon));
        }
        return repo.save(kho);
    }

    public Page<Inventory> sapHet(Long storeId, int trang, int moiTrang) {
        return repo.sapHet(storeId, PageRequest.of(Math.max(0, trang - 1), moiTrang));
    }

    public long demSapHet(Long storeId) {
        return repo.demSapHet(storeId);
    }

    public long demHetHang(Long storeId) {
        return repo.demHetHang(storeId);
    }
}
