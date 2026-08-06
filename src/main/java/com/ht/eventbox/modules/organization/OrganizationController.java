package com.ht.eventbox.modules.organization;

import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Organization;
import com.ht.eventbox.enums.OrganizationRole;
import com.ht.eventbox.modules.organization.dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;

    /*
    API dùng để lấy tất cả các tổ chức (không xài)
    */
    @GetMapping
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:organizations')")
    public ResponseEntity<Response<List<Organization>>> getAll() {
        var res = organizationService.getAll();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả các tổ chức của người dùng hiện tại với vai trò là OWNER (không xài)
    */
    @GetMapping("/me")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:organizations')")
    public ResponseEntity<Response<List<Organization>>> getMyOwn(
            Principal principal
    ) {
        var res = organizationService.getByUserIdAndOrganizationRole(Long.valueOf(principal.getName()), OrganizationRole.OWNER);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả các tổ chức của người dùng hiện tại đang tham gia, dùng cho web ban tổ chức và mobile app giao diện ban tổ chức
    */
    @GetMapping("/me/member")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:organizations')")
    public ResponseEntity<Response<List<Organization>>> getMy(
            Principal principal
    ) {
        var res = organizationService.getByUserId(Long.valueOf(principal.getName()));
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy thông tin của một tổ chức theo ID, dùng cho mobile app giao diện người dùng
    */
    @GetMapping("/{id}/details")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:organizations')")
    public ResponseEntity<Response<OrganizationService.OrganizationDetails>> getDetailsById(
            Principal principal,
            @PathVariable Long id) {
        var res = organizationService.getDetailsById(id);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy thông tin của một tổ chức theo ID, dùng cho web ban tổ chức
    */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:organizations')")
    public ResponseEntity<Response<Organization>> getById(
            Principal principal,
            @PathVariable Long id) {
        var res = organizationService.getById(id);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để cập nhật thông tin của một tổ chức theo ID, dùng cho web ban tổ chức
    */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'update:organizations')")
    public ResponseEntity<Response<Boolean>> updateById(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationDto updateOrganizationDto) {
        var res = organizationService.update(Long.valueOf(principal.getName()), id, updateOrganizationDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để xóa một tổ chức theo ID, dùng cho web ban tổ chức
    */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'delete:organizations')")
    public ResponseEntity<Response<Boolean>> deleteById(
            Principal principal,
            @PathVariable Long id) {
        var res = organizationService.deleteById(Long.valueOf(principal.getName()), id);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.DELETE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để tạo một tổ chức mới, dùng cho web ban tổ chức
    */
    @PostMapping
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'create:organizations')")
    public ResponseEntity<Response<Boolean>> create(Principal principal,
                                                    @Valid @RequestBody CreateOrganizationDto createOrganizationDto) {
        var res = organizationService.create(Long.valueOf(principal.getName()), createOrganizationDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để thêm thành viên vào tổ chức, dùng cho web ban tổ chức
    */
    @PostMapping("/{id}/members")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'update:organizations')")
    public ResponseEntity<Response<Boolean>> addMember(
            Principal principal,
            @Valid @RequestBody AddMemberDto addMemberDto,
            @PathVariable Long id) {
        var res = organizationService.addMember(Long.valueOf(principal.getName()), id, addMemberDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để cập nhật vai trò của thành viên trong tổ chức, dùng cho web ban tổ chức
    */
    @PutMapping("/{id}/members")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'update:organizations')")
    public ResponseEntity<Response<Boolean>> updateMember(
            Principal principal,
            @Valid @RequestBody UpdateMemberDto updateMemberDto,
            @PathVariable Long id) {
        var res = organizationService.updateMember(Long.valueOf(principal.getName()), id, updateMemberDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để xóa thành viên khỏi tổ chức, dùng cho web ban tổ chức
    */
    @PostMapping("/{id}/members/remove")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'update:organizations')")
    public ResponseEntity<Response<Boolean>> removeMember(
            Principal principal,
            @Valid @RequestBody RemoveMemberDto removeMemberDto,
            @PathVariable Long id) {
        var res = organizationService.removeMember(Long.valueOf(principal.getName()), id, removeMemberDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để người dùng đăng ký theo dõi tổ chức (nhận thông báo khi tổ chức phát hành sự kiện mới), dùng cho mobile app giao diện người dùng
    */
    @PostMapping("/{id}/subscribe")
    public ResponseEntity<Response<Boolean>> subscribe(
            Principal principal,
            @PathVariable Long id) {
        var res = organizationService.subscribe(Long.valueOf(principal.getName()), id);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }
}
