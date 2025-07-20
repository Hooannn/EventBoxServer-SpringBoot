package com.ht.eventbox.modules.category;

import com.ht.eventbox.entities.Category;
import com.ht.eventbox.modules.category.dtos.CreateBulkCategoriesDto;
import lombok.*;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public boolean createBulk(CreateBulkCategoriesDto createBulkCategoriesDto) {
        logger.info("Creating bulk categories: {}", createBulkCategoriesDto);
        List<Category> categories = createBulkCategoriesDto.getCategories().stream()
                .map(categoryDto -> Category.builder()
                        .slug(categoryDto.getSlug())
                        .nameVi(categoryDto.getNameVi())
                        .nameEn(categoryDto.getNameEn())
                        .build())
                .toList();
        categoryRepository.saveAll(categories);
        return true;
    }

    public List<Category> getAllFeatured() {
        return categoryRepository.findAllByFeaturedTrueOrderByIdAsc();
    }
}
