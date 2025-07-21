package com.ht.eventbox.modules.ticket;

import com.ht.eventbox.annotations.RequiredPermissions;
import com.ht.eventbox.config.Response;
import com.ht.eventbox.entities.TicketItem;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.modules.ticket.dtos.ValidateTicketItemDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@CrossOrigin
@RequestMapping(path = "/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(TicketController.class);

    private final TicketService ticketService;

    @GetMapping("/items/me")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<Response<List<TicketService.TicketItemDetails>>> getMyTicketItems(
            @RequestAttribute("sub") String sub
    ) {
        var res = ticketService.getTicketItemsByUserIdAndOrderStatusIs(Long.valueOf(sub), OrderStatus.FULFILLED);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @GetMapping("/items/{ticketItemId}")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<Response<TicketService.TicketItemDetails>> getTicketItemById(
            @RequestAttribute("sub") String sub,
            @PathVariable Long ticketItemId) {
        var res = ticketService.getTicketItemById(ticketItemId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @GetMapping("/items/{ticketItemId}/qrcode")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<Response<String>> getTicketItemQrCode(
            @RequestAttribute("sub") String sub,
            @PathVariable String ticketItemId)
    {
        var res = ticketService.getTicketItemQrCode(Long.valueOf(sub), Long.valueOf(ticketItemId));
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @PostMapping("/validate")
    public ResponseEntity<Response<TicketService.TicketItemDetails>> validateTicketItem(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody ValidateTicketItemDto validateTicketItemDto)
    {
        var res = ticketService.validateTicketItem(Long.valueOf(sub), validateTicketItemDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @PostMapping("/traces")
    public ResponseEntity<Response<Boolean>> createTicketItemTrace(
            @RequestAttribute("sub") String sub,
            @Valid @RequestBody ValidateTicketItemDto createTicketItemTraceDto)
    {
        var res = ticketService.createTicketItemTrace(Long.valueOf(sub), createTicketItemTraceDto);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }

    @GetMapping("/shows/{showId}/items")
    @RequiredPermissions({"read:orders"})
    public ResponseEntity<Response<List<TicketItem>>> getTicketItemByShowId(
            @RequestAttribute("sub") String sub,
            @PathVariable Long showId)
    {
        var res = ticketService.getTicketItemByShowId(Long.valueOf(sub), showId);
        return ResponseEntity.ok(
                new Response<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.getReasonPhrase(),
                        res
                )
        );
    }
}
