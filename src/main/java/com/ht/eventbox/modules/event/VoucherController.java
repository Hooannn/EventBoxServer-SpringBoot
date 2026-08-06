package com.ht.eventbox.modules.event;

import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Voucher;
import com.ht.eventbox.modules.category.dtos.CreateCategoryDto;
import com.ht.eventbox.modules.event.dtos.ApplyVoucherDto;
import com.ht.eventbox.modules.event.dtos.CreateVoucherDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {
    private final VoucherService voucherService;

    /*
    API dùng để lấy tất cả lượt sử dụng voucher, id là id của voucher, eventId là id của event, dùng cho web giao diện ban tổ chức
    */
    @GetMapping("/{id}/event/{eventId}/usage")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:vouchers')")
    public ResponseEntity<Response<Long>> getUsage(
            Principal principal,
            @PathVariable Long eventId,
            @PathVariable Long id
    ) {
        var res = voucherService.getUsage(Long.valueOf(principal.getName()), id, eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy voucher hiện tại của một đơn hàng, dùng cho mobile app giao diện người dùng
    */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:vouchers')")
    public ResponseEntity<Response<Voucher>> getByOrderId(
            Principal principal,
            @PathVariable Long orderId
    ) {
        var res = voucherService.getByOrderId(Long.valueOf(principal.getName()), orderId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để áp dụng voucher cho một đơn hàng, dùng cho mobile app giao diện người dùng
    */
    @PostMapping("/order/{orderId}/apply")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:vouchers', 'create:orders')")
    public ResponseEntity<Response<Boolean>> applyByOrderId(
            Principal principal,
            @Valid @RequestBody ApplyVoucherDto applyVoucherDto,
            @PathVariable Long orderId
    ) {
        var res = voucherService.applyByOrderId(Long.valueOf(principal.getName()), orderId, applyVoucherDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để huỷ bỏ voucher cho một đơn hàng, dùng cho mobile app giao diện người dùng
    */
    @PostMapping("/order/{orderId}/remove")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:vouchers', 'create:orders')")
    public ResponseEntity<Response<Boolean>> removeByOrderId(
            Principal principal,
            @PathVariable Long orderId
    ) {
        var res = voucherService.removeByOrderId(Long.valueOf(principal.getName()), orderId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả vouchers của một event, dùng cho web giao diện ban tổ chức
    */
    @GetMapping("/event/{eventId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:vouchers')")
    public ResponseEntity<Response<List<Voucher>>> getAllByEventId(
            Principal principal,
            @PathVariable Long eventId
    ) {
        var res = voucherService.getAllByEventId(Long.valueOf(principal.getName()), eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả public vouchers của một event, dùng cho mobile app giao diện người dùng
    */
    @GetMapping("/event/{eventId}/public")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'read:vouchers')")
    public ResponseEntity<Response<List<Voucher>>> getAllPublicByEventId(
            @PathVariable Long eventId
    ) {
        var res = voucherService.getAllPublicByEventId(eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để tạo mới một voucher, tạo mới voucher từ body CreateVoucherDto, dùng cho web giao diện ban tổ chức
    */
    @PostMapping("/event/{eventId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'create:vouchers')")
    public ResponseEntity<Response<Boolean>> createByEventId(
            Principal principal,
            @Valid @RequestBody CreateVoucherDto createVoucherDto,
            @PathVariable Long eventId
    ) {
        var res = voucherService.createByEventId(Long.valueOf(principal.getName()), eventId, createVoucherDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để cập nhật một voucher, cập nhật voucher từ body CreateVoucherDto, id là id của voucher cần cập nhật, dùng cho web giao diện ban tổ chức
    */
    @PutMapping("/{id}/event/{eventId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'update:vouchers')")
    public ResponseEntity<Response<Boolean>> updateByEventId(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody CreateVoucherDto createVoucherDto,
            @PathVariable Long eventId
    ) {
        var res = voucherService.updateByEventId(Long.valueOf(principal.getName()), id, eventId, createVoucherDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để xóa một voucher, id là id của voucher cần xóa, dùng cho web giao diện ban tổ chức
     */
    @DeleteMapping("/{id}/event/{eventId}")
    @PreAuthorize("@permissionAuthorization.hasAll(authentication, 'delete:vouchers')")
    public ResponseEntity<Response<Boolean>> deleteByEventId(
            Principal principal,
            @PathVariable Long id,
            @PathVariable Long eventId
    ) {
        var res = voucherService.deleteByEventId(Long.valueOf(principal.getName()), id, eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
