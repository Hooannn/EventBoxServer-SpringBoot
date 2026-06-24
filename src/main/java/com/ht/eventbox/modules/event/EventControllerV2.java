package com.ht.eventbox.modules.event;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.QueryResponse;
import com.ht.eventbox.entities.Event;
import com.ht.eventbox.enums.EventStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(path = "/api/v2/events")
@RequiredArgsConstructor
public class EventControllerV2 {
    private final EventService eventService;

    @GetMapping
    @RequiredPermissions({"read:events", "access:admin"})
    public ResponseEntity<QueryResponse<Event>> getAll(Pageable pageable) {
        var res = eventService.getAllByStatusIn(
                List.of(EventStatus.PENDING, EventStatus.PUBLISHED),
                pageable
        );
        return ResponseEntity.ok(
                QueryResponse.from(res, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase())
        );
    }
}
