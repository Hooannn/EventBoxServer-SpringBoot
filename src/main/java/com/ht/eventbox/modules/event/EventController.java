package com.ht.eventbox.modules.event;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.modules.event.dtos.CreateEventDto;
import com.ht.eventbox.modules.event.dtos.UpdateEventDto;
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
}
