package com.smartwash.category;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Danh mục gốc, kèm sẵn danh mục con để dựng cây một lần. */
    @EntityGraph(attributePaths = {"children"})
    List<Category> findByParentIsNullOrderBySortOrderAscNameAsc();

    List<Category> findByParentIdOrderBySortOrderAscNameAsc(Long parentId);

    long countByParentId(Long parentId);

    @Query("SELECT c FROM Category c WHERE c.status = 'ACTIVE' ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> dangHien();

    /** Toàn bộ danh mục kèm cha, dùng cho ô chọn trong form. */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> tatCaKemCha();

    /**
     * Id của danh mục và mọi danh mục con của nó.
     * Chỉ đi 1 cấp vì giao diện chỉ dựng cây 2 cấp.
     */
    @Query("SELECT c.id FROM Category c WHERE c.id = :id OR c.parent.id = :id")
    List<Long> idVaCon(@Param("id") Long id);
}
