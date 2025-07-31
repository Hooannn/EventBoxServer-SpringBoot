package com.ht.eventbox.modules.organization;

import com.ht.eventbox.annotations.RequiredPermissions;
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
    @RequiredPermissions({"read:organizations"})
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
    @RequiredPermissions({"read:organizations"})
    public ResponseEntity<Response<List<Organization>>> getMyOwn(
            @RequestAttribute("sub") String sub
    ) {
        var res = organizationService.getByUserIdAndOrganizationRole(Long.valueOf(sub), OrganizationRole.OWNER);
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
    @RequiredPermissions({"read:organizations"})
    public ResponseEntity<Response<List<Organization>>> getMy(
            @RequestAttribute("sub") String sub
    ) {
        var res = organizationService.getByUserId(Long.valueOf(sub));
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
    @RequiredPermissions({"read:organizations"})
    public ResponseEntity<Response<Organization>> getById(
            @RequestAttribute("sub") String sub,
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
    @RequiredPermissions({"update:organizations"})
    public ResponseEntity<Response<Boolean>> updateById(
            @RequestAttribute("sub") String sub,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationDto updateOrganizationDto) {
        var res = organizationService.update(Long.valueOf(sub), id, updateOrganizationDto);
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
    @RequiredPermissions({"delete:organizations"})
    public ResponseEntity<Response<Boolean>> deleteById(
            @RequestAttribute("sub") String sub,
            @PathVariable Long id) {
        var res = organizationService.deleteById(Long.valueOf(sub), id);
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
    @RequiredPermissions({"create:organizations"})
    public ResponseEntity<Response<Boolean>> create(@RequestAttribute("sub") String sub,
                                                    @Valid @RequestBody CreateOrganizationDto createOrganizationDto) {
        var res = organizationService.create(Long.valueOf(sub), createOrganizationDto);
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
    @RequiredPermissions({"update:organizations"})
    public ResponseEntity<Response<Boolean>> addMember(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody AddMemberDto addMemberDto,
            @PathVariable Long id) {
        var res = organizationService.addMember(Long.valueOf(sub), id, addMemberDto);
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
    @RequiredPermissions({"update:organizations"})
    public ResponseEntity<Response<Boolean>> updateMember(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody UpdateMemberDto updateMemberDto,
            @PathVariable Long id) {
        var res = organizationService.updateMember(Long.valueOf(sub), id, updateMemberDto);
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
    @RequiredPermissions({"update:organizations"})
    public ResponseEntity<Response<Boolean>> removeMember(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody RemoveMemberDto removeMemberDto,
            @PathVariable Long id) {
        var res = organizationService.removeMember(Long.valueOf(sub), id, removeMemberDto);
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
            @RequestAttribute("sub") String sub,
            @PathVariable Long id) {
        var res = organizationService.subscribe(Long.valueOf(sub), id);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }
}
