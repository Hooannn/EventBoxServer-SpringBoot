package com.ht.eventbox.modules.user;

import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;
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

    /*
    API dùng để lấy tất cả người dùng, dùng cho web admin
    */
    @GetMapping
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:users')")
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

    /*
    API dùng để lấy tất cả các vai trò (roles) của hệ thống, dùng cho web admin
    */
    @GetMapping("/roles")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:roles')")
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

    /*
    API dùng để tạo một vai trò mới, dùng cho web admin
    */
    @PostMapping("/roles")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'create:roles')")
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

    /*
    API dùng để cập nhật một vai trò, dùng cho web admin
    */
    @PutMapping("/roles/{roleId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'update:roles')")
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

    /*
    API dùng để xóa một vai trò, dùng cho web admin
    */
    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'delete:roles')")
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

    /*
    API dùng để lấy tất cả quyền (permissions) của hệ thống, dùng cho web admin
    */
    @GetMapping("/roles/permissions")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:permissions')")
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

    /*
    API dùng để tạo một quyền mới, dùng cho web admin
    */
    @PostMapping("/roles/permissions")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'create:permissions')")
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

    /*
    API dùng để cập nhật một quyền, dùng cho web admin
    */
    @PutMapping("/roles/permissions/{permissionId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'update:roles')")
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

    /*
    API dùng để xóa một quyền, dùng cho web admin
    */
    @DeleteMapping("/roles/permissions/{permissionId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'delete:roles')")
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

    /*
    API dùng để cập nhật vai trò của người dùng, dùng cho web admin
    */
    @PutMapping("/{userId}/role")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'update:users')")
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
    public ResponseEntity<Response<User>> getAuthenticated(Principal principal) {
        var res = userService.getById(Long.valueOf(principal.getName()));
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để đổi mật khẩu, dùng cho web và mobile app
    */
    @PutMapping("/me/change-password")
    public ResponseEntity<Response<Boolean>> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordDto changePasswordDto) {
        var res = userService.changePassword(Long.valueOf(principal.getName()), changePasswordDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để cập nhật thông tin cá nhân của người dùng, dùng cho web và mobile app
    */
    @PutMapping("/me/update")
    public ResponseEntity<Response<Boolean>> updateInformation(
            Principal principal,
            @Valid @RequestBody UpdateInformationDto updateInformationDto) {
        var res = userService.updateInformation(Long.valueOf(principal.getName()), updateInformationDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để tạo một người dùng mới (không sử dụng)
    */
    @PostMapping
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'create:users')")
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

    /*
    API dùng để tạo một người dùng mới (không sử dụng)
    */
    @PostMapping("/bulk")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'create:users')")
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

    /*
    API dùng để cập nhật các firebase cloud messaging token (dùng để push noti) của người dùng, dùng cho mobile app
    */
    @PutMapping("/fcm/tokens")
    public ResponseEntity<Response<Boolean>> updateFCMTokens(
            Principal principal,
            @Valid @RequestBody UpdateFCMTokensDto updateFCMTokensDto
    ) {
        var res = userService.updateFCMTokens(Long.valueOf(principal.getName()), updateFCMTokensDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
