package com.ht.eventbox.modules.event;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.QueryResponse;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.constant.Constant;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.enums.EventStatus;
import com.ht.eventbox.modules.event.dtos.EventOverviewDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v2/events")
@RequiredArgsConstructor
public class EventControllerV2 {
    private final EventService eventService;

    @GetMapping
    @RequiredPermissions({ "read:events", "access:admin" })
    public ResponseEntity<QueryResponse<Event>> getAll(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        var res = eventService.getAllByStatusIn(
                List.of(EventStatus.PENDING, EventStatus.PUBLISHED),
                search,
                pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase()));
    }

    @GetMapping("/pending")
    @RequiredPermissions({ "read:events", "access:admin" })
    public ResponseEntity<QueryResponse<Event>> getAllPending(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        var res = eventService.getAllPending(search, pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase()));
    }

    @GetMapping("/published")
    @RequiredPermissions({ "read:events", "access:admin" })
    public ResponseEntity<QueryResponse<Event>> getAllPublished(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        var res = eventService.getAllPublished(search, pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase()));
    }

    @GetMapping("/ended")
    @RequiredPermissions({ "read:events", "access:admin" })
    public ResponseEntity<QueryResponse<Event>> getAllEnded(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        var res = eventService.getAllEnded(search, pageable);
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase()));
    }

    @GetMapping("/overview")
    @RequiredPermissions({ "read:events", "access:admin" })
    public ResponseEntity<Response<EventOverviewDto>> getOverview(
            @RequestParam(required = false) String search) {
        var res = eventService.getOverview(search);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res));
    }
}
