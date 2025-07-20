package com.ht.eventbox.modules.category;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.entities.Category;
import com.ht.eventbox.modules.category.dtos.CreateBulkCategoriesDto;
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
