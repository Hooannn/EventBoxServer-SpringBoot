package com.ht.eventbox.modules.user;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import com.ht.eventbox.modules.user.dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
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

    @GetMapping("/roles")
    @RequiredPermissions({"read:roles"})
    public ResponseEntity<Response<List<Role>>> getAllRoles() {
        var res = userService.getAllRoles();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @PostMapping("/roles")
    @RequiredPermissions({"create:roles"})
    public ResponseEntity<Response<Boolean>> createRole(
            @Valid @RequestBody CreateRoleDto createRoleDto
    ) {
        var res = userService.createRole(createRoleDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    @PutMapping("/roles/{roleId}")
    @RequiredPermissions({"update:roles"})
    public ResponseEntity<Response<Boolean>> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleDto updateRoleDto
    ) {
        var res = userService.updateRole(roleId, updateRoleDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @DeleteMapping("/roles/{roleId}")
    @RequiredPermissions({"delete:roles"})
    public ResponseEntity<Response<Boolean>> deleteRole(
            @PathVariable Long roleId
    ) {
        var res = userService.deleteRole(roleId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @GetMapping("/roles/permissions")
    @RequiredPermissions({"read:permissions"})
    public ResponseEntity<Response<List<Permission>>> getAllPermissions() {
        var res = userService.getAllPermissions();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @PostMapping("/roles/permissions")
    @RequiredPermissions({"create:permissions"})
    public ResponseEntity<Response<Boolean>> createPermission(
            @Valid @RequestBody CreatePermissionDto createPermissionDto
    ) {
        var res = userService.createPermission(createPermissionDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    @PutMapping("/roles/permissions/{permissionId}")
    @RequiredPermissions({"update:roles"})
    public ResponseEntity<Response<Boolean>> updatePermission(
            @PathVariable Long permissionId,
            @Valid @RequestBody UpdatePermissionDto updatePermissionDto
    ) {
        var res = userService.updatePermission(permissionId, updatePermissionDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @DeleteMapping("/roles/permissions/{permissionId}")
    @RequiredPermissions({"delete:roles"})
    public ResponseEntity<Response<Boolean>> deletePermission(
            @PathVariable Long permissionId
    ) {
        var res = userService.deletePermission(permissionId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @PutMapping("/{userId}/role")
    @RequiredPermissions({"update:users"})
    public ResponseEntity<Response<Boolean>> updateRole(
            @Valid @RequestBody UpdateUserRoleDto updateUserRoleDto,
            @PathVariable Long userId) {
        var res = userService.updateUserRole(userId, updateUserRoleDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
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
