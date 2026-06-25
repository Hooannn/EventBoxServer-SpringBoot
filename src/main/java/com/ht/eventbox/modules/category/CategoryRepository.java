package com.ht.eventbox.modules.category;

import com.ht.eventbox.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByFeaturedTrueOrderByIdAsc();

    List<Category> findAllByOrderByIdAsc();

    Page<Category> findAllByOrderByIdAsc(Pageable pageable);

    @Query("""
            SELECT c
            FROM Category c
            WHERE :search IS NULL
               OR :search = ''
               OR LOWER(c.nameVi) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY c.id ASC
            """)
    Page<Category> searchAllByOrderByIdAsc(@Param("search") String search, Pageable pageable);
}
