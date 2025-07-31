package com.ht.eventbox.modules.event;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.entities.EventShow;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.modules.event.dtos.CreateEventDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventTagsDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    /*
    API dùng để lấy tất cả các sự kiện đang chờ duyệt hoặc đã được phát hành, dùng cho admin web
    */
    @GetMapping
    @RequiredPermissions({"read:events", "access:admin"})
    public ResponseEntity<Response<List<Event>>> getAll() {
        var res = eventService.getAllByStatusIn(
                List.of(EventStatus.PENDING, EventStatus.PUBLISHED)
        );
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để duyệt sự kiện (đồng ý phát hành), dùng cho admin web
    */
    @PostMapping("/{eventId}/admin/publish")
    @RequiredPermissions({"update:events", "access:admin"})
    public ResponseEntity<Response<Boolean>> publishByAdmin(@PathVariable Long eventId)
    {
        var res = eventService.publishByAdmin(eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để duyệt sự kiện (từ chối phát hành), dùng cho admin web
    */
    @PostMapping("/{eventId}/admin/archive")
    @RequiredPermissions({"update:events", "access:admin"})
    public ResponseEntity<Response<Boolean>> archiveByAdmin(@PathVariable Long eventId)
    {
        var res = eventService.archiveByAdmin(eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để cập nhật tags cho sự kiện (nổi bật, xu hướng), dùng cho admin web
    */
    @PutMapping("/{eventId}/admin/tags")
    @RequiredPermissions({"update:events", "access:admin"})
    public ResponseEntity<Response<Boolean>> updateTags(
            @RequestAttribute("sub") String sub,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventTagsDto updateEventTagsDto)
    {
        var res = eventService.updateTags(eventId, updateEventTagsDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để tạo sự kiện mới, dùng cho web ban tổ chức
    */
    @PostMapping
    @RequiredPermissions({"create:events"})
    public ResponseEntity<Response<Boolean>> create(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody CreateEventDto createEventDto)
    {
        var res = eventService.create(Long.valueOf(sub), createEventDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để cập nhật sự kiện khi chưa được duyệt, dùng cho web ban tổ chức
    */
    @PutMapping("/{eventId}")
    @RequiredPermissions({"update:events"})
    public ResponseEntity<Response<Boolean>> update(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody UpdateEventDto updateEventDto,
            @PathVariable Long eventId)
    {
        var res = eventService.update(Long.valueOf(sub), eventId, updateEventDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để lưu trữ sự kiện khi chưa được duyệt, dùng cho web ban tổ chức
    */
    @PostMapping("/{eventId}/archive")
    @RequiredPermissions({"update:events"})
    public ResponseEntity<Response<Boolean>> archive(
            @RequestAttribute("sub") String sub,
            @PathVariable Long eventId)
    {
        var res = eventService.archive(Long.valueOf(sub), eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để tạm ẩn sự kiện khi sự kiện đã được duyệt, dùng cho web ban tổ chức
    */
    @PostMapping("/{eventId}/inactive")
    @RequiredPermissions({"update:events"})
    public ResponseEntity<Response<Boolean>> inactive(
            @RequestAttribute("sub") String sub,
            @PathVariable Long eventId)
    {
        var res = eventService.inactive(Long.valueOf(sub), eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để bật lại sự kiện đang tạm ẩn khi sự kiện đã được duyệt, dùng cho web ban tổ chức
    */
    @PostMapping("/{eventId}/active")
    @RequiredPermissions({"update:events"})
    public ResponseEntity<Response<Boolean>> active(
            @RequestAttribute("sub") String sub,
            @PathVariable Long eventId)
    {
        var res = eventService.active(Long.valueOf(sub), eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        Constant.SuccessCode.UPDATE_SUCCESSFULLY,
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả các sự kiện (trạng thái khác lưu trữ) của một tổ chức, dùng cho web ban tổ chức
    */
    @GetMapping("/organization/{organizationId}")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<List<Event>>> getByOrganizationId(@PathVariable Long organizationId) {
        var res = eventService.getByOrganizationIdAndStatusIsNot(organizationId, EventStatus.ARCHIVED);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả các sự kiện được phát hành của một tổ chức, dùng cho mobile app giao diện của ban tổ chức
    */
    @GetMapping("/organization/{organizationId}/published")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<List<Event>>> getPublishedByOrganizationId(@PathVariable Long organizationId) {
        var res = eventService.getByOrganizationIdAndStatusIs(organizationId, EventStatus.PUBLISHED);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }


    /*
    API dùng để lấy sự kiện theo ID (hiện tại không xài)
    */
    @GetMapping("/{eventId}")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<Event>> getById(@PathVariable Long eventId) {
        var res = eventService.getByIdAndStatusIsNot(eventId, EventStatus.ARCHIVED);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả các chương trình của sự kiện theo ID, dùng cho web ban tổ chức
    */
    @GetMapping("/{eventId}/shows")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<List<EventShow>>> getShowsById(@PathVariable Long eventId) {
        var res = eventService.getShowsById(eventId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy sự kiện theo ID, dùng cho mobile app giao diện của người dùng
    */
    @GetMapping("/public/{eventId}")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<Event>> getPublicById(@PathVariable Long eventId) {
        var res = eventService.getWithRealStockByIdAndStatusIsNot(eventId, EventStatus.ARCHIVED);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy các sự kiện nổi bật, xu hướng, mới phát hành, dùng cho mobile app giao diện của người dùng (trang chủ)
    */
    @GetMapping("/discovery")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<EventService.DiscoveryEvents>> getDiscovery() {
        var res = eventService.getDiscovery();
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để tìm kiếm các sự kiện theo từ khóa, tỉnh thành, và danh mục, dùng cho mobile app giao diện của người dùng
    query: tìm theo title hoặc description
    province: tìm theo tên thành phố (có thể là null)
    categories: tìm theo danh sách id danh mục (có thể là null)
    */
    @GetMapping("/search")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<List<Event>>> search(
            @RequestParam(value = "q") String query,
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "categories", required = false) List<Long> categories
    ) {
        var res = eventService.search(
                query,
                province,
                categories
        );
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    /*
    API dùng để lấy tất cả các sự kiện theo id của danh mục, dùng cho mobile app giao diện của người dùng (trang chủ)
    */
    @GetMapping("/categories/{categoryId}")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<List<Event>>> getByCategoryId(@PathVariable Long categoryId) {
        var res = eventService.getAllByCategoriesIdAndStatusIs(
                categoryId,
                EventStatus.PUBLISHED
        );
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
