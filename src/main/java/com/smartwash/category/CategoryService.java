package com.smartwash.category;

import com.smartwash.brand.BrandNameNormalizer;
import com.smartwash.common.LoiNghiepVu;
import com.smartwash.common.Messages;
import com.smartwash.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Danh mục sản phẩm dạng cây (đặc tả mục 4.4, 7.6). */
@Service
public class CategoryService {

    private final CategoryRepository repo;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository repo, ProductRepository productRepository) {
        this.repo = repo;
        this.productRepository = productRepository;
    }

    // ------------------------------------------------------------- tra cứu

    public Category timHoacBao(Long id) {
        return repo.findById(id).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy danh mục."));
    }

    public Category theoSlug(String slug) {
        return repo.findBySlug(slug).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy danh mục."));
    }

    /** Cây đầy đủ: danh mục gốc kèm danh mục con. */
    public List<Category> cay() {
        return repo.findByParentIsNullOrderBySortOrderAscNameAsc();
    }

    /** Cây chỉ gồm danh mục đang hiện, dùng cho menu khách. */
    public List<Category> cayHien() {
        List<Category> ra = new ArrayList<>();
        for (Category goc : cay()) {
            if (goc.isActive()) {
                ra.add(goc);
            }
        }
        return ra;
    }

    /** Danh sách phẳng cho ô chọn trong form, tên con có tiền tố "— ". */
    public List<Category> choChon() {
        return repo.tatCaKemCha();
    }

    /** Danh mục này và mọi danh mục con — lọc sản phẩm theo nhánh. */
    public List<Long> nhanh(Long categoryId) {
        return categoryId == null ? null : repo.idVaCon(categoryId);
    }

    // --------------------------------------------------------- tạo và sửa

    @Transactional
    public Category tao(String ten, Long parentId, String moTa, String anh, Integer thuTu, boolean hien) {
        if (ten == null || ten.trim().isEmpty()) {
            throw new LoiNghiepVu("name", Messages.required("tên danh mục"));
        }

        Category c = new Category();
        c.setName(ten.trim());
        c.setSlug(slugDuyNhat(BrandNameNormalizer.slugify(ten), null));
        c.setParent(parentId == null ? null : timHoacBao(parentId));
        c.setDescription(moTa);
        c.setImage(anh);
        c.setSortOrder(thuTu == null ? 0 : thuTu);
        c.setStatus(hien ? Category.ACTIVE : Category.HIDDEN);
        return repo.save(c);
    }

    @Transactional
    public Category capNhat(Long id, String ten, Long parentId, String moTa, String anh,
                            Integer thuTu, boolean hien) {
        Category c = timHoacBao(id);
        if (ten == null || ten.trim().isEmpty()) {
            throw new LoiNghiepVu("name", Messages.required("tên danh mục"));
        }

        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new LoiNghiepVu("parentId", "Danh mục không thể là cha của chính nó.");
            }
            Category cha = timHoacBao(parentId);
            // Chặn vòng: cha mới không được nằm trong nhánh con của danh mục này
            if (repo.idVaCon(id).contains(parentId)) {
                throw new LoiNghiepVu("parentId",
                        "Không thể chuyển vào chính danh mục con của nó.");
            }
            c.setParent(cha);
        } else {
            c.setParent(null);
        }

        if (!ten.trim().equals(c.getName())) {
            c.setName(ten.trim());
            c.setSlug(slugDuyNhat(BrandNameNormalizer.slugify(ten), id));
        }
        c.setDescription(moTa);
        if (anh != null) {
            c.setImage(anh);
        }
        c.setSortOrder(thuTu == null ? 0 : thuTu);
        c.setStatus(hien ? Category.ACTIVE : Category.HIDDEN);
        return repo.save(c);
    }

    /**
     * Xoá danh mục.
     *
     * Chặn khi còn danh mục con hoặc còn sản phẩm: xoá được thì sản phẩm sẽ
     * mồ côi, mà products.category_id là NOT NULL nên CSDL cũng sẽ từ chối —
     * báo lỗi rõ ràng ở đây tốt hơn để người dùng gặp lỗi SQL.
     */
    @Transactional
    public void xoa(Long id) {
        Category c = timHoacBao(id);

        long soCon = repo.countByParentId(id);
        if (soCon > 0) {
            throw new LoiNghiepVu("Danh mục \"" + c.getName() + "\" còn " + soCon
                    + " danh mục con. Hãy xoá hoặc chuyển chúng đi trước.");
        }

        long soSanPham = productRepository.countByCategoryId(id);
        if (soSanPham > 0) {
            throw new LoiNghiepVu("Danh mục \"" + c.getName() + "\" còn " + soSanPham
                    + " sản phẩm. Hãy chuyển sản phẩm sang danh mục khác trước, "
                    + "hoặc ẩn danh mục thay vì xoá.");
        }

        repo.delete(c);
    }

    @Transactional
    public void doiTrangThai(Long id, boolean hien) {
        Category c = timHoacBao(id);
        c.setStatus(hien ? Category.ACTIVE : Category.HIDDEN);
        repo.save(c);
    }

    /** Nạp số sản phẩm cho từng danh mục để hiển thị trong bảng quản trị. */
    public void napSoSanPham(List<Category> ds) {
        for (Category c : ds) {
            c.setProductCount(productRepository.countByCategoryId(c.getId()));
            if (c.getChildren() != null) {
                for (Category con : c.getChildren()) {
                    con.setProductCount(productRepository.countByCategoryId(con.getId()));
                }
            }
        }
    }

    private String slugDuyNhat(String goc, Long boQuaId) {
        String s = goc == null || goc.isBlank() ? "danh-muc" : goc;
        if (!trungSlug(s, boQuaId)) {
            return s;
        }
        for (int i = 2; i < 500; i++) {
            String thu = s + "-" + i;
            if (!trungSlug(thu, boQuaId)) {
                return thu;
            }
        }
        return s + "-" + System.currentTimeMillis();
    }

    private boolean trungSlug(String slug, Long boQuaId) {
        return repo.findBySlug(slug)
                .map(c -> boQuaId == null || !c.getId().equals(boQuaId))
                .orElse(false);
    }
}
