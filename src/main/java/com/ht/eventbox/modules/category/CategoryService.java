package com.ht.eventbox.modules.category;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Category;
import com.ht.eventbox.modules.category.dtos.CreateBulkCategoriesDto;
import com.ht.eventbox.modules.category.dtos.CreateCategoryDto;
import com.ht.eventbox.modules.event.EventRepository;
import lombok.*;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public List<Category> getAll() {
        // Lấy tất cả các category từ database và sắp xếp theo id tăng dần
        return categoryRepository.findAllByOrderByIdAsc();
    }

    public boolean createBulk(CreateBulkCategoriesDto createBulkCategoriesDto) {
        logger.info("Creating bulk categories: {}", createBulkCategoriesDto);
        List<Category> categories = createBulkCategoriesDto.getCategories().stream()
                .map(categoryDto -> Category.builder()
                        .slug(UUID.randomUUID().toString())
                        .nameVi(categoryDto.getNameVi())
                        .nameEn(categoryDto.getNameEn())
                        .build())
                .toList();
        categoryRepository.saveAll(categories);
        return true;
    }

    public List<Category> getAllFeatured() {
        // Lấy tất cả các category có featured = true và sắp xếp theo id tăng dần
        return categoryRepository.findAllByFeaturedTrueOrderByIdAsc();
    }

    public boolean create(CreateCategoryDto createCategoryDto) {
        // Tạo với slug là một UUID ngẫu nhiên (không sử dụng)
        Category category = Category.builder()
                .slug(UUID.randomUUID().toString())
                .nameVi(createCategoryDto.getNameVi())
                .nameEn(createCategoryDto.getNameEn())
                .featured(createCategoryDto.isFeatured())
                .build();

        categoryRepository.save(category);
        return true;
    }

    public boolean update(Long categoryId, CreateCategoryDto createCategoryDto) {
        // Kiểm tra xem category có tồn tại không
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new HttpException(
                        Constant.ErrorCode.CATEGORY_NOT_FOUND,
                        HttpStatus.BAD_REQUEST
                ));

        category.setNameVi(createCategoryDto.getNameVi());
        category.setNameEn(createCategoryDto.getNameEn());
        category.setFeatured(createCategoryDto.isFeatured());

        categoryRepository.save(category);
        return true;
    }

    @Transactional
    public boolean delete(Long id) {
        // Kiểm tra xem category có tồn tại không
        if (!categoryRepository.existsById(id)) {
            throw new HttpException(
                    Constant.ErrorCode.CATEGORY_NOT_FOUND,
                    HttpStatus.BAD_REQUEST
            );
        }

        // Kiểm tra xem category có đang được sử dụng trong event không
        if (eventRepository.existsByCategoriesId(id)) {
            throw new HttpException(
                    Constant.ErrorCode.CATEGORY_IN_USE,
                    HttpStatus.BAD_REQUEST
            );
        }

        categoryRepository.deleteById(id);
        return true;
    }
}
