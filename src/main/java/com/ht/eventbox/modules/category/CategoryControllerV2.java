package com.ht.eventbox.modules.category;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.QueryResponse;
import com.ht.eventbox.entities.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v2/categories")
@RequiredArgsConstructor
public class CategoryControllerV2 {
    private final CategoryService categoryService;

    @GetMapping
    @RequiredPermissions({"read:categories"})
    public ResponseEntity<QueryResponse<Category>> getAll(Pageable pageable) {
        var res = categoryService.getAll(pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase())
        );
    }
}
