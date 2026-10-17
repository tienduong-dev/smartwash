package com.smartwash.product;

import com.smartwash.brand.Brand;
import com.smartwash.brand.BrandRepository;
import com.smartwash.category.Category;
import com.smartwash.category.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tra cứu sản phẩm cho khách: tìm kiếm, lọc, sắp xếp (đặc tả mục 5.2, 5.5).
 *
 * Tách khỏi ProductService vì hai bên khác hẳn nhau: bên kia là nghiệp vụ ghi
 * của người bán và quản trị, bên này chỉ đọc và chỉ thấy hàng đang bán.
 */
@Service
public class CatalogService {

    /** Các cách sắp xếp cho khách chọn. */
    public static final String MOI_NHAT   = "moi-nhat";
    public static final String GIA_TANG   = "gia-tang";
    public static final String GIA_GIAM   = "gia-giam";
    public static final String BAN_CHAY   = "ban-chay";
    public static final String DANH_GIA   = "danh-gia";

    private final ProductRepository repo;
    private final CategoryService categoryService;
    private final BrandRepository brandRepository;

    public CatalogService(ProductRepository repo,
                          CategoryService categoryService,
                          BrandRepository brandRepository) {
        this.repo = repo;
        this.categoryService = categoryService;
        this.brandRepository = brandRepository;
    }

    /** Bộ lọc khách gửi lên từ query string. */
    public static class BoLoc {
        public String q;
        public Long categoryId;
        public Long brandId;
        public Long storeId;
        public String doorType;
        public Boolean inverter;
        public BigDecimal capacityFrom;
        public BigDecimal capacityTo;
        public BigDecimal priceFrom;
        public BigDecimal priceTo;
        public String sort = MOI_NHAT;

        /** Có tiêu chí nào đang bật không — để hiện nút "Xoá bộ lọc". */
        public boolean coTieuChi() {
            return categoryId != null || brandId != null || storeId != null
                    || (doorType != null && !doorType.isBlank())
                    || inverter != null
                    || capacityFrom != null || capacityTo != null
                    || priceFrom != null || priceTo != null
                    || (q != null && !q.isBlank());
        }
    }

    public Page<Product> tim(BoLoc f, int trang, int moiTrang) {
        List<Long> nhanh = categoryService.nhanh(f.categoryId);
        Pageable trangBan = PageRequest.of(Math.max(0, trang - 1), moiTrang, sapXep(f.sort));

        return repo.timKiemCongKhai(
                rong(f.q), nhanh, f.brandId, f.storeId,
                rong(f.doorType), f.inverter,
                f.capacityFrom, f.capacityTo,
                f.priceFrom, f.priceTo,
                trangBan);
    }

    private Sort sapXep(String kieu) {
        return switch (kieu == null ? MOI_NHAT : kieu) {
            case GIA_TANG -> Sort.by(Sort.Direction.ASC, "price");
            case GIA_GIAM -> Sort.by(Sort.Direction.DESC, "price");
            case BAN_CHAY -> Sort.by(Sort.Direction.DESC, "soldCount");
            case DANH_GIA -> Sort.by(Sort.Direction.DESC, "ratingAvg")
                                 .and(Sort.by(Sort.Direction.DESC, "reviewCount"));
            default       -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    public List<Category> danhMucLoc() {
        return categoryService.cayHien();
    }

    public List<Brand> thuongHieuLoc() {
        return brandRepository.findByStatusOrderByNameAsc(Brand.ACTIVE);
    }

    /** [giá thấp nhất, giá cao nhất] của hàng đang bán; null nếu chưa có hàng. */
    public BigDecimal[] khoangGia() {
        List<Object[]> r = repo.khoangGia();
        if (r.isEmpty() || r.get(0) == null || r.get(0)[0] == null) {
            return null;
        }
        return new BigDecimal[]{(BigDecimal) r.get(0)[0], (BigDecimal) r.get(0)[1]};
    }

    public List<Product> noiBat(int soLuong) {
        return repo.noiBat(PageRequest.of(0, soLuong));
    }

    public List<Product> gioiThieuThem(Product p, int soLuong) {
        if (p.getCategory() == null) {
            return List.of();
        }
        return repo.cungDanhMuc(p.getCategory().getId(), p.getId(), PageRequest.of(0, soLuong));
    }

    /**
     * Tăng lượt xem.
     *
     * Giao dịch riêng và không ảnh hưởng tới việc hiển thị: đếm hụt một lượt
     * xem không đáng để hỏng cả trang chi tiết.
     */
    @Transactional
    public void ghiLuotXem(Long productId) {
        try {
            repo.tangLuotXem(productId);
        } catch (RuntimeException e) {
            // bỏ qua có chủ ý
        }
    }

    private String rong(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
