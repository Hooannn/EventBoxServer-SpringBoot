package com.ht.eventbox.modules.category;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Category;
import com.ht.eventbox.modules.category.dtos.CreateBulkCategoriesDto;
import com.ht.eventbox.modules.category.dtos.CreateCategoryDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/categories")
@RequiredArgsConstructor
@CrossOrigin
public class CategoryController {
    private final CategoryService categoryService;

    /*
    API dùng để lấy tất cả category, dùng cho admin web
    */
    @GetMapping
    @RequiredPermissions({"read:categories"})
    public ResponseEntity<Response<List<Category>>> getAll() {
        var res = categoryService.getAll();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để tạo mới một category, tạo mới category từ body CreateCategoryDto, dùng cho admin web
    */
    @PostMapping
    @RequiredPermissions({"create:categories"})
    public ResponseEntity<Response<Boolean>> create(@Valid @RequestBody CreateCategoryDto createCategoryDto) {
        var res = categoryService.create(createCategoryDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để cập nhật một category, cập nhật category từ body CreateCategoryDto, id là id của category cần cập nhật, dùng cho admin web
    */
    @PutMapping("/{id}")
    @RequiredPermissions({"update:categories"})
    public ResponseEntity<Response<Boolean>> update(@PathVariable Long id,
                                                   @Valid @RequestBody CreateCategoryDto createCategoryDto) {
        var res = categoryService.update(id, createCategoryDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để xóa một category, id là id của category cần xóa, dùng cho admin web
     */
    @DeleteMapping("/{id}")
    @RequiredPermissions({"delete:categories"})
    public ResponseEntity<Response<Boolean>> delete(@PathVariable Long id) {
        var res = categoryService.delete(id);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả category có featured = true, sắp xếp theo id tăng dần, dùng để hiện trên app mobile
     */
    @GetMapping("/featured")
    @RequiredPermissions({"read:categories"})
    public ResponseEntity<Response<List<Category>>> getAllFeatured() {
        var res = categoryService.getAllFeatured();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để tạo mới nhiều category cùng lúc, từ body CreateBulkCategoriesDto (hiện không sử dụng trong app)
     */
    @PostMapping("/bulk")
    @RequiredPermissions({"create:categories"})
    public ResponseEntity<Response<Boolean>> createBulk(@Valid @RequestBody CreateBulkCategoriesDto createBulkCategoriesDto) {
        var res = categoryService.createBulk(createBulkCategoriesDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }
}
