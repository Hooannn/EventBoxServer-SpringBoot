package com.ht.eventbox.modules.category;

import com.ht.eventbox.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByFeaturedTrueOrderByIdAsc();
}
