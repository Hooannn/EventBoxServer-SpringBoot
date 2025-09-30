package com.ht.eventbox.modules.event;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Voucher;
import com.ht.eventbox.modules.category.dtos.CreateCategoryDto;
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
    @RequiredPermissions({"read:vouchers"})
    public ResponseEntity<Response<Long>> getUsage(
            @RequestAttribute("sub") String sub,
            @PathVariable Long eventId,
            @PathVariable Long id
    ) {
        var res = voucherService.getUsage(Long.valueOf(sub), id, eventId);
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
    @RequiredPermissions({"read:vouchers"})
    public ResponseEntity<Response<List<Voucher>>> getAllByEventId(
            @RequestAttribute("sub") String sub,
            @PathVariable Long eventId
    ) {
        var res = voucherService.getAllByEventId(Long.valueOf(sub), eventId);
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
    @RequiredPermissions({"read:vouchers"})
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
    @RequiredPermissions({"create:vouchers"})
    public ResponseEntity<Response<Boolean>> createByEventId(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody CreateVoucherDto createVoucherDto,
            @PathVariable Long eventId
    ) {
        var res = voucherService.createByEventId(Long.valueOf(sub), eventId, createVoucherDto);
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
    @RequiredPermissions({"update:vouchers"})
    public ResponseEntity<Response<Boolean>> updateByEventId(
            @RequestAttribute("sub") String sub,
            @PathVariable Long id,
            @Valid @RequestBody CreateVoucherDto createVoucherDto,
            @PathVariable Long eventId
    ) {
        var res = voucherService.updateByEventId(Long.valueOf(sub), id, eventId, createVoucherDto);
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
    @RequiredPermissions({"delete:vouchers"})
    public ResponseEntity<Response<Boolean>> deleteByEventId(
            @RequestAttribute("sub") String sub,
            @PathVariable Long id,
            @PathVariable Long eventId
    ) {
        var res = voucherService.deleteByEventId(Long.valueOf(sub), id, eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
