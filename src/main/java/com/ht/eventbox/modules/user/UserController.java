package com.ht.eventbox.modules.user;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.entities.Category;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.modules.category.CategoryService;
import com.ht.eventbox.modules.category.dtos.CreateBulkCategoriesDto;
import com.ht.eventbox.modules.category.dtos.UpdateFCMTokensDto;
import com.ht.eventbox.modules.user.dtos.CreateBulkUsersDto;
import com.ht.eventbox.modules.user.dtos.CreateUserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping(path = "/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    @RequiredPermissions({"read:users"})
    public ResponseEntity<Response<List<User>>> getAll() {
        var res = userService.getAll();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Response<User>> getAuthenticated(@RequestAttribute("sub") String sub) {
        var res = userService.getById(Long.valueOf(sub));
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @PostMapping
    @RequiredPermissions({"create:users"})
    public ResponseEntity<Response<Boolean>> create(
            @Valid @RequestBody CreateUserDto createUserDto
    ) {
        var res = userService.create(createUserDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    @PostMapping("/bulk")
    @RequiredPermissions({"create:users"})
    public ResponseEntity<Response<Boolean>> createBulk(
            @Valid @RequestBody CreateBulkUsersDto createBulkUsersDto
    ) {
        var res = userService.createBulk(createBulkUsersDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    @PutMapping("/fcm/tokens")
    public ResponseEntity<Response<Boolean>> updateFCMTokens(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody UpdateFCMTokensDto updateFCMTokensDto
    ) {
        var res = userService.updateFCMTokens(Long.valueOf(sub), updateFCMTokensDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
