package com.ht.eventbox.modules.event;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.modules.event.dtos.CreateEventDto;
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

    @PostMapping
    @RequiredPermissions({"create:events"})
    public ResponseEntity<Response<Boolean>> create(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody CreateEventDto createEventDto) {

        var res = eventService.create(Long.valueOf(sub), createEventDto);
        return ResponseEntity.created(null).body(
                new Response<>(
                        HttpStatus.CREATED.value(),
                        HttpStatus.CREATED.getReasonPhrase(),
                        res
                )
        );
    }

    @GetMapping("/organization/{organizationId}")
    @RequiredPermissions({"read:events"})
    public ResponseEntity<Response<List<Event>>> getByOrganizationId(@PathVariable Long organizationId) {
        var res = eventService.getByOrganizationId(organizationId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
